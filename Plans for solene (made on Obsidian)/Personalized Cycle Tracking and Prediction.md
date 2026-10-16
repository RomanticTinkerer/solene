Done using Gradient Boosting Trees (training XGBoost on the NFP dataset for period predictions)

Granular tracking, XGBoost needs more derivatives of the NFP dataset to train on for more granular tracking (present on folder titled "Training Data (headstart), both model and data.)

---

# Developed plan (October 2026)

## Where it stands

Shipped in the native app (`android/`):

- Two XGBoost models, trained on `FedCycleData071012.csv` (159 people, 1,665 cycles): next cycle length and ovulation day.
- `export_ondevice_models.py` exports the trees to JSON (`app/src/main/assets/models/`); `XgbOnDeviceModel.kt` walks them on the phone. No network, no native library.
- Retrained in October 2026 with past-only features and 5-fold cross-validation split by person (details in the findings below). RMSE in days:

| Model | Users with 1–2 cycles | Users with 3+ cycles | Fallback with 1–2 cycles | Fallback with 3+ cycles |
|---|---|---|---|---|
| Cycle length | 3.36 | 3.15 | 3.62 (own mean) | **3.05** (own mean) |
| Ovulation day | 3.34 | 2.99 | 3.41 (14-day luteal rule) | **2.91** (14-day luteal rule) |

- **How the app chooses** (`CyclePredictionEngine`):
  - with 1–2 completed cycles, the model;
  - from 3 cycles on, the user's own mean;
  - with none, the stated average.
- An ovulation day or luteal length the user entered always beats the model.
- Predictions and Stats are built from logged bleeding, not just the onboarding answers.

The models are modest. They help new users a little; after that, the user's own history does the work.

## Finding: the "derivatives" add nothing

`FedCycleData071012_deriv1.csv` and `_deriv2.csv` are exact copies of the base file. Every column is identical; only `ClientID` gets a `_d1` / `_d2` suffix.

- They give the model no new information. More copies will not make tracking more granular.
- `train_xgboost.py` concatenates all three files and splits rows at random. Copies of the same cycle land on both sides of the split, so the scores it prints are inflated.
- `export_ondevice_models.py`, which produces the shipped models, de-duplicates and splits by person, but it has a different leak (next section). The numbers above are optimistic.

## Finding: person-level columns leak, and the app's model gate is backwards

In the base CSV, `MeanCycleLength`, `MeanMensesLength`, `Age`, `Height`, `Weight`, `BMI` and the other person-level columns are filled only on each person's **first** cycle row (141 of 1,665 rows). There, `MeanCycleLength` equals the mean of **all** that person's cycles, including the future cycle being predicted.

The app fills the same features differently: a mean of past cycles only, with age and so on always present. Training and the app therefore disagree.

**Fix:** build every history feature from the past only, exactly as the app does:

- an expanding mean of cycle lengths and of menses lengths up to the current cycle;
- person-level columns forward-filled from the first row;
- optionally, the variation so far, the previous cycle's length, and the number of cycles so far.

**Evaluation:** use 5-fold cross-validation split by person, and always compare against the plain baseline "the person's mean so far". A single 80/20 split with about 30 test people is too noisy to decide anything.

Experiment, October 2026 (5-fold by person, same hyperparameters as the shipped model). RMSE in days for the next cycle length:

| | All rows | Users with 1–2 cycles | Users with 3+ cycles |
|---|---|---|---|
| Shipped features (leaky) | 3.45 | 3.24* | 3.50 |
| Past-only features | 3.23 | 3.34 | 3.20 |
| Past-only + variation, previous length, count | 3.16 | 3.32 | 3.12 |
| Baseline: person's mean so far | 3.16 | 3.62 | **3.05** |
| Baseline: last cycle | 4.01 | 3.90 | 4.03 |

\* Flattered by the leak: first-cycle rows carry the future mean.

For the ovulation day, the person's mean of their measured ovulation days (2.46 with 3+ cycles) beats every model. The app can only measure ovulation once the symptothermal rules are ported.

What this means:

1. The model helps only while someone has 1–2 cycles of history, by about 0.3 days over their own mean. From 3 cycles on, the person's mean is as good or better.
2. The app used to do the reverse, consulting the model only from 3 cycles on. **Done:** the gate is flipped, so the model is used with 1–2 cycles and the personal mean from 3 on. The per-user backtest (below) can later override either.
3. The ovulation model is barely better than the 14-day luteal rule (3.34 vs 3.41 days), and the user's mean of measured ovulation days beats both. The symptothermal port is the real improvement there.

**Done in `export_ondevice_models.py`:**

- Features are built from past cycles only: running means, and person-level columns forward-filled.
- The menses features describe the period that opens the cycle being predicted, as the app supplies them.
- Ovulation day, luteal length and peak-day columns are dropped until the app measures them.
- Cycle pairs across a gap in cycle numbering are skipped.
- Each run prints 5-fold grouped scores next to the app's fallbacks and stores them in the meta JSON (`metrics.by_history`, `metrics.baselines`).
- It writes to both `android/app/src/main/assets/models` and the React Native `assets/models`.

Caveats: no hyperparameter tuning was done, and differences under about 0.1 days are within noise for 148 people.

Decisions:

1. Stop generating copies. `export_ondevice_models.py` is the training script; `train_xgboost.py` (copies, random split) should be deleted.
2. Get granularity from new signals, not more rows:
   - the user's own logged cycles (already used);
   - temperature and mucus observations (symptothermal rules, below);
   - wearable resting heart rate and night temperature ([[Wearable Integration]]).
3. If augmentation is ever wanted, it must change values (e.g. jitter lengths by the person's own variation) and still split by the original person. Measure before and after. The ceiling is 159 people, not the row count.

## Done: drip's symptothermal rules (October 2026)

drip's `sympto` package (Sensiplan: temperature shift with both exceptions, mucus peak, cervix as the alternative sign, the 5-day and minus-8-day rules) is ported to Kotlin.

- `data/fertility/Sympto.kt` is the port. It keeps sympto's AGPL header and is credited in the licence dialog.
- `data/fertility/FertilityRules.kt` is drip's adapter: it splits logged days into cycles, maps them to sympto's units, and turns the result into the Home card's phase and text (drip's own wording).
- `getFertilityStatus()` no longer uses calendar arithmetic. Only the rules can say "infertile"; the next-period prediction is shown separately.
- The **NFP Method** setting became **Fertility signs**: temperature + cervical mucus (default), temperature + cervix, or off (periods only, hides the card). Older stored values map to the default, or to off for "None".
- Mucus now has Sensiplan's five categories. "Nothing" (Ø) and "Moist" (f) were missing; without them a moist day could only be logged as dry, which wrongly extends the infertile days.
- A temperature can be excluded from the rules (fever, alcohol, broken sleep, late reading). It is still stored and exported.

Correctness check: `scripts/make-sympto-fixture.js` runs the original JavaScript on sympto's own example cycles (with and without a previous cycle) and on 160 generated histories. It writes `test/fixtures/sympto-reference.json` (226 cases, covering all three temperature rules, cervix, and the minus-8 rule both shortening and lengthening the pre-ovulatory phase). `SymptoReferenceTest` requires the Kotlin port to match every case.

One deliberate difference from drip: sympto counts cycle days for the minus-8 rule with js-joda's `LocalDate.compareTo`, which returns the month difference when the dates are in different months. drip then computes a negative length and drops the infertile pre-ovulatory phase whenever a previous cycle's first high temperature fell in a later month than its start. This changes 60 of the 226 reference cases. The port counts real days, and the fixture generator patches `compareTo` the same way. drip's result is the more cautious one, but it is not what Sensiplan describes.

Still open:

- A confirmed temperature shift should replace the model's ovulation estimate for the current cycle, and measured ovulation days should feed `typicalOvulationDay` (the personal mean beats every model, see the table above).
- The calendar could show each day's phase; today only the Home card does.
- drip lets bleeding be excluded from cycle detection; Solene does not yet (see [[Developer Options]]).

## Then: show the user how accurate their predictions are

- Backtest on the user's own data: predict each logged cycle from the cycles before it. Show on Stats: "Your predictions were within ±2 days for 5 of your last 7 cycles."
- It is a pure function over the existing `CycleHistory` output, so it needs no new storage.
- Use the backtest to pick the model or the personal mean per user. This replaces the fixed "fewer than 3 cycles" rule.

## Out of scope

- Training or fine-tuning trees on the phone. One person's dozen cycles cannot train XGBoost; backtest-based selection gets most of the benefit.
- Cloud training, data donation, telemetry.

## Risks

- Remembered cycle lengths from onboarding are weaker than logged ones. Keep the `enteredAtSetup` flag; down-weight them if the backtest shows they hurt.
- The dataset comes from a single NFP study, so it may not represent irregular cycles, PCOS, or perimenopause. Personal history should keep winning for those users, and the backtest will show it.
