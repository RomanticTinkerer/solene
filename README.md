# Solene

Solene is a period and fertility tracker for Android, and everything stays on your phone. It started as a fork of [drip](https://gitlab.com/bloodyhealth/drip). Then it got rebuilt as a native Kotlin app with Material 3 Expressive, plus cycle prediction that runs right on the device.

> **Still a work in progress.** There's no release yet, so you'll need to build it yourself (see below).
>
> Solene isn't a medical device, and it isn't birth control. Predictions are estimates. The fertility status just applies the symptothermal rules to what you log, so always double-check it yourself.

## Privacy

- **It can't go online.** The app doesn't ask for internet permission, so it has no way to send your data anywhere. It also skips accounts, analytics and ads.
- **Backups stay off.** Cloud backup and phone-to-phone transfer are both turned off. So your cycle data only leaves the phone if you export it yourself.
- **Health Connect is optional and read-only.** If you allow it, Solene reads vitals like resting heart rate, body temperature, steps and sleep. It never writes anything back.
- **You can take your data with you.** Settings can export everything as CSV or JSON.

## Features

- **Day log:** bleeding, basal temperature, cervical mucus, cervix, mood, pain, desire and a note. Took your temperature while sick or after a bad night? You can exclude it from the fertility rules, and it still stays in your log.
- **Fertility status:** a Kotlin port of drip's symptothermal (Sensiplan) rules. It finds the temperature shift, the mucus or cervix shift, and the infertile days at the start and end of your cycle. In Settings you pick temperature + mucus, temperature + cervix, or turn it off and just track periods.
- **Period and ovulation prediction:** an on-device XGBoost model while your history is short, then your own average (more on that below).
- **Calendar and stats:** logged and predicted bleeding, cycle lengths, variation and period lengths. Stats come from the cycles you logged, plus any past cycle lengths you entered at setup (those are marked). Sample data never gets mixed in.
- **Reminders:** a daily logging reminder and a "period expected soon" reminder. Both start switched off.
- **Vitals:** the You tab shows your latest Health Connect readings.

## How prediction works

Solene ships with two small XGBoost models, one for cycle length and one for ovulation day. They're stored as JSON inside the app and run on the phone. They were trained on a public natural family planning dataset (`FedCycleData071012.csv`, 159 participants), using only what was known before each cycle started. Testing used 5-fold cross-validation grouped by person, so nobody shows up in both training and testing. Hence the error numbers below are honest.

Which estimate you get depends on how much history you have:

| Completed cycles you have logged | Next period from |
|---|---|
| none | the average cycle length you entered at setup |
| 1–2 | the model (cross-validated error 3.36 days, vs 3.62 for your own average) |
| 3 or more | your own average (3.05 days, vs 3.15 for the model) |

Actually, if you know your usual ovulation day or luteal phase length, enter it. Solene then uses that for the ovulation estimate instead of the model.

## Building

You need JDK 17 and the Android SDK (platform 36). The Gradle wrapper downloads Gradle 8.11.1 for you.

```sh
cd android
./gradlew :app:assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew :app:installDebug           # or install on a connected device
./gradlew :app:testDebugUnitTest      # unit tests
```

The app runs on Android 8.0 (API 26) or newer.

## Tests

- **Android unit tests** (`./gradlew :app:testDebugUnitTest`) cover prediction, cycle history, reminders, temperature input and the fertility rules. The fertility rules have to match drip's original JavaScript on 226 reference cases in `test/fixtures/sympto-reference.json`.
- **JavaScript tests** (`npm install`, then `npx mocha --recursive --require @babel/register test`) are drip's tests, plus a check that the exported models give the same predictions as the reference fixture.

To regenerate the fertility reference cases, run `node scripts/make-sympto-fixture.js` (it needs `npm install` first).

## Retraining the models

```sh
cd "Training Data (headstart)"
python -m venv venv && venv/bin/pip install pandas scikit-learn xgboost
venv/bin/python export_ondevice_models.py
```

This prints the cross-validation results next to the simple baselines and writes the models to `android/app/src/main/assets/models/`. After that, run `venv/bin/python make_reference_fixture.py` to refresh the fixture the tests compare against.

## Repository layout

- `android/`: the Solene app (Kotlin, Jetpack Compose, Hilt, DataStore).
- `Training Data (headstart)/`: the dataset and the training scripts.
- `test/`: drip's JavaScript tests and the fixtures shared with the Android tests.
- `scripts/make-sympto-fixture.js`: generates the fertility reference cases.
- `Plans for solene (made on Obsidian)/`: the roadmap. Next up are developer options, wearable temperature history, a daily well-being check, a health article library, pregnancy tracking and partner sharing.
- Everything else at the top level (`components/`, `lib/`, `db/`, `ios/`, …) is drip's original React Native app. It stays around for reference and for its tests, but it isn't built anymore. `CONTRIBUTING.md`, `RELEASE.md` and `CHANGELOG.md` are still drip's too.

## Credits and licence

Solene is licensed under the [GNU General Public License v3.0 or later](LICENSE).

- [drip](https://gitlab.com/bloodyhealth/drip) by Bloody Health GbR, GPL-3.0-or-later. Solene started from its code, and the fertility status texts are drip's.
- The fertility rules in `android/app/src/main/java/com/solene/app/data/fertility/Sympto.kt` are a port of [sympto](https://gitlab.com/bloodyhealth/sympto) by Julia Friesel, Marie Kochsiek and Tina Baumann, licensed AGPL-3.0-or-later. The port keeps that licence. It differs in one spot: when two dates fall in different months, sympto counts some cycle days in months, but the port counts real days.
