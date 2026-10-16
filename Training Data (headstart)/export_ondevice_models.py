#!/usr/bin/env python3
"""
Train Solene on-device XGBoost models and export trees as JSON.

Exports a compact feature set that the app can fill from logged cycle data,
then dumps booster trees so the native app (`CyclePredictionEngine.kt`) and
the JS runtime (`lib/prediction`) can evaluate predictions fully on-device.

Every feature is built the way the app builds it at prediction time: from
past cycles only, never from the person's whole series.
"""

from __future__ import annotations

import json
import os
from pathlib import Path

import numpy as np
import pandas as pd
import xgboost as xgb
from sklearn.metrics import mean_squared_error
from sklearn.model_selection import GroupKFold

ROOT = Path(__file__).resolve().parent
OUT_DIR = ROOT / "exported_models"
APP_ASSETS = [
    ROOT.parent / "android" / "app" / "src" / "main" / "assets" / "models",
    ROOT.parent / "assets" / "models",
]

MENSES_SCORE_COLUMNS = [
    f"MensesScoreDay{day}" for day in ["One", "Two", "Three", "Four", "Five", "Six", "Seven"]
]
# Describe the period that opens the cycle being predicted; the app knows it by then.
MENSES_COLUMNS = ["LengthofMenses", "TotalMensesScore"] + MENSES_SCORE_COLUMNS
# Only filled on each person's first row in the CSV.
PERSON_COLUMNS = [
    "Age", "Height", "Weight", "BMI",
    "ReproductiveCategory", "Numberpreg", "Livingkids", "Breastfeeding",
]
# Mirrors CyclePredictionEngine.buildFeatures. Ovulation day, luteal length and
# peak day stay out until the app measures them (symptothermal port).
FEATURE_COLUMNS = [
    "LengthofCycle",
    "MeanCycleLength",
    "MeanMensesLength",
    "MeanBleedingIntensity",
] + MENSES_COLUMNS + PERSON_COLUMNS

# The app uses the model below this many completed cycles, the personal mean from it on.
MIN_CYCLES_FOR_PERSONAL_MEAN = 3
TYPICAL_LUTEAL_LENGTH = 14

TARGETS = {
    "Next_LengthofCycle": "LengthofCycle",
    "Next_EstimatedDayofOvulation": "EstimatedDayofOvulation",
}


def load_frames() -> pd.DataFrame:
    """Load the original NFP charting study.

    `FedCycleData071012_deriv1.csv` and `_deriv2.csv` are the same 159
    clients with remapped IDs (`nfp8122_d1`, …) and nearly identical
    values. Concatenating them triples the sample without adding people,
    so they are left out of on-device training.
    """
    path = ROOT / "FedCycleData071012.csv"
    if not path.exists():
        raise SystemExit("FedCycleData071012.csv not found.")
    df = pd.read_csv(path)
    df = df.replace([" ", ""], np.nan)
    numeric = ["CycleNumber", "LengthofCycle", "EstimatedDayofOvulation"] + MENSES_COLUMNS + PERSON_COLUMNS
    df[numeric] = df[numeric].apply(pd.to_numeric, errors="coerce")
    df = df.drop_duplicates(subset=["ClientID", "CycleNumber"], keep="first")
    df = df.sort_values(by=["ClientID", "CycleNumber"])
    return df.reset_index(drop=True)


def training_rows(df: pd.DataFrame, source_col: str) -> pd.DataFrame:
    """One row per completed cycle: features known when the next one starts, plus its target."""
    by_person = df.groupby("ClientID")

    def running_mean(col: str) -> pd.Series:
        return by_person[col].transform(lambda s: s.expanding().mean())

    rows = pd.DataFrame({"ClientID": df["ClientID"]})
    rows["LengthofCycle"] = df["LengthofCycle"]
    rows["MeanCycleLength"] = running_mean("LengthofCycle")
    # The opening period of the next cycle, and means that include it, as in the app.
    rows[MENSES_COLUMNS] = by_person[MENSES_COLUMNS].shift(-1)
    rows["MeanMensesLength"] = running_mean("LengthofMenses").groupby(df["ClientID"]).shift(-1)
    rows["MeanBleedingIntensity"] = running_mean("TotalMensesScore").groupby(df["ClientID"]).shift(-1)
    rows[PERSON_COLUMNS] = by_person[PERSON_COLUMNS].ffill()
    rows["CompletedCycles"] = by_person.cumcount() + 1
    rows["MeanOvulationDay"] = running_mean("EstimatedDayofOvulation")
    rows["target"] = by_person[source_col].shift(-1)

    next_is_consecutive = by_person["CycleNumber"].shift(-1) == df["CycleNumber"] + 1
    usable = next_is_consecutive & rows["target"].notna() & rows["LengthofCycle"].notna()
    return rows.loc[usable].reset_index(drop=True)


def new_model() -> xgb.XGBRegressor:
    return xgb.XGBRegressor(
        n_estimators=80,
        max_depth=4,
        learning_rate=0.08,
        subsample=0.9,
        colsample_bytree=0.9,
        random_state=42,
        objective="reg:squarederror",
    )


def rmse(pred: np.ndarray, y: np.ndarray) -> float:
    ok = ~np.isnan(pred)
    return float(np.sqrt(mean_squared_error(y[ok], pred[ok])))


def parse_base_score(model: xgb.XGBRegressor) -> float:
    config = json.loads(model.get_booster().save_config())
    raw_base = config["learner"]["learner_model_param"]["base_score"]
    if isinstance(raw_base, str):
        parsed = json.loads(raw_base)
        return float(parsed[0] if isinstance(parsed, list) else parsed)
    return float(raw_base)


def walk_tree(node: dict, features: dict[str, float]) -> float:
    if "leaf" in node:
        return float(node["leaf"])
    value = features.get(node["split"], float("nan"))
    if value is None or (isinstance(value, float) and np.isnan(value)):
        next_id = node["missing"]
    elif float(value) < float(node["split_condition"]):
        next_id = node["yes"]
    else:
        next_id = node["no"]
    for child in node["children"]:
        if child["nodeid"] == next_id:
            return walk_tree(child, features)
    raise RuntimeError(f"Missing child node {next_id}")


def predict_from_dump(payload: dict, features: dict[str, float]) -> float:
    total = float(payload["base_score"])
    for tree in payload["trees"]:
        total += walk_tree(tree, features)
    return total


def export_booster(
    model: xgb.XGBRegressor,
    feature_names: list[str],
    path: Path,
    sample_row: pd.Series,
) -> None:
    raw = model.get_booster().get_dump(dump_format="json")
    trees = [json.loads(tree) for tree in raw]
    payload = {
        "format": "solene-xgb-json-v1",
        "base_score": parse_base_score(model),
        "feature_names": feature_names,
        "trees": trees,
        "missing_value": None,
    }

    sample_features = {
        name: (float(sample_row[name]) if pd.notna(sample_row[name]) else float("nan"))
        for name in feature_names
    }
    native = float(model.predict(pd.DataFrame([sample_features]))[0])
    walked = predict_from_dump(payload, sample_features)
    if abs(native - walked) > 1e-4:
        raise SystemExit(
            f"Export verification failed for {path.name}: native={native} walked={walked}"
        )
    print(f"Verified dump walker matches native predict ({native:.6f})")

    path.write_text(json.dumps(payload))
    print(f"Wrote {path} ({path.stat().st_size} bytes, {len(trees)} trees)")


def train_one(df: pd.DataFrame, next_target: str, source_col: str) -> None:
    rows = training_rows(df, source_col)
    X, y = rows[FEATURE_COLUMNS], rows["target"].to_numpy()
    few = (rows["CompletedCycles"] < MIN_CYCLES_FOR_PERSONAL_MEAN).to_numpy()

    # Out-of-fold predictions: every person is scored by a model that never saw them.
    pred = np.zeros(len(y))
    for train_idx, test_idx in GroupKFold(n_splits=5).split(X, y, rows["ClientID"]):
        fold = new_model()
        fold.fit(X.iloc[train_idx], y[train_idx])
        pred[test_idx] = fold.predict(X.iloc[test_idx])

    # What the app falls back to without the model.
    mean_so_far = rows["MeanCycleLength"].to_numpy()
    baselines = {"personal_mean": mean_so_far}
    if source_col == "EstimatedDayofOvulation":
        baselines = {
            "luteal_estimate": mean_so_far - TYPICAL_LUTEAL_LENGTH,
            "measured_ovulation_mean": rows["MeanOvulationDay"].to_numpy(),
        }

    def scores(p: np.ndarray) -> dict[str, float]:
        return {
            "all": rmse(p, y),
            f"under_{MIN_CYCLES_FOR_PERSONAL_MEAN}_cycles": rmse(p[few], y[few]),
            f"{MIN_CYCLES_FOR_PERSONAL_MEAN}_plus_cycles": rmse(p[~few], y[~few]),
        }

    model_scores = scores(pred)
    baseline_scores = {name: scores(p) for name, p in baselines.items()}
    print(f"\n=== {next_target} ===")
    print(f"{len(rows)} rows / {rows['ClientID'].nunique()} people, 5-fold by person, RMSE in days")
    for name, s in {"model": model_scores, **baseline_scores}.items():
        print(f"  {name:26}" + "  ".join(f"{k}={v:.2f}" for k, v in s.items()))

    model = new_model()
    model.fit(X, y)

    defaults = {
        name: (None if pd.isna(val) else float(val))
        for name, val in X.median(numeric_only=True).items()
    }

    stem = "cycle_length" if "LengthofCycle" in next_target else "ovulation_day"
    meta = {
        "target": next_target,
        "feature_names": FEATURE_COLUMNS,
        "feature_defaults": defaults,
        "metrics": {
            "rmse": model_scores["all"],
            "by_history": model_scores,
            "baselines": baseline_scores,
            "rows": int(len(rows)),
            "clients": int(rows["ClientID"].nunique()),
            "split": "5-fold grouped by person",
            "dataset": "FedCycleData071012.csv",
        },
        "clamps": {
            "Next_LengthofCycle": {"min": 18, "max": 60},
            "Next_EstimatedDayofOvulation": {"min": 8, "max": 40},
        }.get(next_target, {"min": 1, "max": 60}),
    }
    for out in [OUT_DIR, *APP_ASSETS]:
        out.mkdir(parents=True, exist_ok=True)
        (out / f"{stem}_meta.json").write_text(json.dumps(meta, indent=2))
        export_booster(model, FEATURE_COLUMNS, out / f"{stem}.json", X.iloc[0])


def main() -> None:
    os.chdir(ROOT)
    df = load_frames()
    for next_target, source in TARGETS.items():
        train_one(df, next_target, source)
    print(f"\nModels written to {', '.join(str(p) for p in APP_ASSETS)}")
    print("Run make_reference_fixture.py to refresh the shared runtime test fixture.")


if __name__ == "__main__":
    main()
