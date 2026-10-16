#!/usr/bin/env python3
"""
Generate a reference fixture for the JavaScript booster runtime.

`export_ondevice_models.py` verifies its JSON dump against XGBoost's native
`predict()`. This script reuses the same walking logic to record expected
outputs for a spread of feature vectors, so `test/prediction-xgboost.spec.js`
can assert that the JS runtime stays numerically identical to the Python one.
"""

from __future__ import annotations

import json
import math
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MODELS = ROOT / "exported_models"
FIXTURE = ROOT.parent / "test" / "fixtures" / "xgboost-reference.json"

MODELS_BY_NAME = {
    "cycleLength": "cycle_length",
    "ovulationDay": "ovulation_day",
}


def walk_tree(node: dict, features: dict) -> float:
    if "leaf" in node:
        return float(node["leaf"])
    value = features.get(node["split"])
    if value is None or (isinstance(value, float) and math.isnan(value)):
        next_id = node["missing"]
    elif float(value) < float(node["split_condition"]):
        next_id = node["yes"]
    else:
        next_id = node["no"]
    for child in node["children"]:
        if child["nodeid"] == next_id:
            return walk_tree(child, features)
    raise RuntimeError(f"Missing child node {next_id}")


def predict(payload: dict, features: dict) -> float:
    total = float(payload["base_score"])
    for tree in payload["trees"]:
        total += walk_tree(tree, features)
    return total


def make_cases(meta: dict, rng: random.Random) -> list[dict]:
    names = meta["feature_names"]
    defaults = meta["feature_defaults"]

    cases = [
        {"label": "all defaults", "features": dict(defaults)},
        {"label": "all missing", "features": {name: None for name in names}},
    ]

    # One case per feature, dropped to missing, to exercise every `missing` branch.
    for name in names:
        features = dict(defaults)
        features[name] = None
        cases.append({"label": f"missing {name}", "features": features})

    # Randomised vectors around the trained medians.
    for i in range(12):
        features = {}
        for name in names:
            base = defaults.get(name)
            if base is None:
                features[name] = None
            elif rng.random() < 0.15:
                features[name] = None
            else:
                features[name] = round(base * rng.uniform(0.5, 1.6), 4)
        cases.append({"label": f"random {i}", "features": features})

    return cases


def main() -> None:
    rng = random.Random(20240614)
    fixture = {}

    for js_name, stem in MODELS_BY_NAME.items():
        payload = json.loads((MODELS / f"{stem}.json").read_text())
        meta = json.loads((MODELS / f"{stem}_meta.json").read_text())

        cases = make_cases(meta, rng)
        for case in cases:
            case["expected"] = predict(payload, case["features"])

        fixture[js_name] = {
            "clamps": meta["clamps"],
            "featureNames": meta["feature_names"],
            "cases": cases,
        }
        print(f"{js_name}: {len(cases)} cases")

    FIXTURE.parent.mkdir(parents=True, exist_ok=True)
    FIXTURE.write_text(json.dumps(fixture, indent=1))
    print(f"Wrote {FIXTURE}")


if __name__ == "__main__":
    main()
