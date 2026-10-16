*fork drip, study all the period trackers and features in their paid subscription apps, and integrate into the fork, and publish it, open-source.*

**DESIGN LANGUAGE: MATERIAL 3 EXPRESSIVE**
[[Personalized Cycle Tracking and Prediction]]
[[Pregnancy Tracking (Detailed)]]
[[Health Content Library]]
[[Daily well-being plan]]
[[Partner Sharing]]
[[Wearable Integration]]
[[Developer Options]]

---

# Status and build order (October 2026)

All work targets the native Kotlin app in `android/`.

## Status

| Plan | In the app today | Next step |
|---|---|---|
| [[Personalized Cycle Tracking and Prediction]] | On-device XGBoost, predictions from logged history, Stats, logging, reminders, drip's symptothermal rules | Measured ovulation feeds predictions; backtest per user |
| [[Developer Options]] | Not started | 7-tap unlock, simulated "today" |
| [[Wearable Integration]] | Latest-vitals card on the You tab (Health Connect) | Daily history, temperature prefill |
| [[Daily well-being plan]] | Not started (steps and sleep already readable) | Evening step check |
| [[Health Content Library]] | Not started (note created) | Port drip's symptom-info texts |
| [[Pregnancy Tracking (Detailed)]] | Not started | Pregnancy mode, contraction timer, kick counter |
| [[Partner Sharing]] | "Coming soon" placeholder | QR snapshot share |

## Build order

1. **Developer Options.** Small, and the simulated "today" lets every later feature be tested without waiting weeks for a real cycle.
2. ~~**Symptothermal port**~~ (done, October 2026; see Personalized Cycle Tracking and Prediction). The fertile window now has a real basis, the calendar-based "infertile" labels are gone, and logged temperatures have a purpose.
3. **Wearable Integration.** Temperature prefill builds on step 2.
4. **Daily well-being plan.** Needs the wearable history.
5. **Health Content Library.** Linked from well-being suggestions and pregnancy tracking.
6. **Pregnancy Tracking.** Pregnancy mode first; the 3D torso last.
7. **Partner Sharing.** The most security work, so it lands once the data it shares is stable.

## Rules for every plan

- **Offline-first.** No accounts, no analytics, no network calls except between a user's own paired devices.
- **FOSS dependencies only** (keeps F-Droid possible). No Google Play Services: no Nearby, no Fit, no ML Kit.
- **Not a medical device.** Say "estimate", "visual", "information". Never call a day "infertile" from calendar maths alone.
- **Licences:** drip is GPL-3.0-or-later and the `sympto` rules are AGPL-3.0-or-later. Bundled articles and assets need compatible licences and must be credited.
- **Design:** Material 3 Expressive throughout.
