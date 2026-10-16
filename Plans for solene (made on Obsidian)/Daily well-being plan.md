*Daily well-being suggestions based on data pulled from [[Wearable Integration]]*

Including suggestions on:

Locomotion (if steps haven't been hit on basis of Cycle position)
Exercise (if calories haven't been burnt till on the daily goal)
etc.


Options to (on the reminder notification/screen)
to include symptoms on why you weren't able to perform or hit limit, so you log on symptoms of menstruation or you could log lack of morale.

---

# Developed plan (October 2026)

## What exists to build on

- **Health Connect:** steps today and last night's sleep are already read (`HealthConnectRepository`). See [[Wearable Integration]].
- **Reminders:** daily alarms, notification channels, permission handling, and rescheduling on boot or time change (`notifications/ReminderScheduler.kt`). A well-being check is a third alarm on the same scheduler.
- **Logging:** today's log already has `Mood` (including `TIRED`) and `PainType` (including `CRAMPS`), and `saveCycleDay` stores a day.

## v1 scope

### One evening check, not a stream of nags

- One check per day at a user-chosen time (default 19:00). Off by default, like the other reminders.
- If the goals are met, nothing is posted.
- If today's log already explains a shortfall (pain or tiredness), nothing is posted either.

### Signals and goals

| Signal | Source | Used how |
|---|---|---|
| Steps | Health Connect aggregate | Compared with the step goal |
| Active calories | Health Connect (`ActiveCaloriesBurnedRecord`, new permission) | Compared with the calorie goal |
| Sleep | Health Connect | Shown for context, never nagged about |

- The user sets the goals. There are no defaults pretending to be medical advice.
- **Cycle position:** an optional multiplier per phase (menstrual, follicular, fertile window, luteal). All default to 1.0, so nothing changes unless the user chooses, e.g. 0.7 during their period.
  - Evidence for phase-specific exercise targets is mixed, so the app lets people adjust and doesn't prescribe.
  - The phase comes from the existing prediction, or from symptothermal rules once ported ([[Personalized Cycle Tracking and Prediction]]).
- **Wording:** short and factual, e.g. "4,200 of 7,000 steps today. A 15-minute walk would close most of the gap." No calorie-shaming language, no streaks.

### "Why not?" quick log (from the original idea)

Android shows at most three action buttons on a notification, so:

| Button | Logs to today |
|---|---|
| Cramps | `PainType.CRAMPS` |
| Tired | `Mood.TIRED` |
| Low motivation | new `Mood.UNMOTIVATED` value |
| (tap the notification body) | Opens today's full day log |

- "Lack of morale" doesn't fit any existing mood, so it gets one new enum value. CSV/JSON export picks it up automatically through the enum name.
- Buttons go to a broadcast receiver that merges the value into today's entry; it never overwrites existing fields. Then the notification is dismissed.
- The same three buttons appear on an in-app card on Home after the check time, for people who keep notifications off.

## Data

- No health data is copied into Solene. Steps, calories and sleep are read from Health Connect at check time.
- New settings in DataStore: check enabled, check time, step goal, calorie goal, and four phase multipliers.

## Constraint: background reads

The check runs from an alarm, with no app on screen.

- Health Connect only allows background reads with `READ_HEALTH_DATA_IN_BACKGROUND`, and only where the feature is available (`HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND`).
- **Fallback when it is not available:** post a neutral "How was today?" notification with the same three buttons, and show the step/calorie comparison when the app opens.

## Milestones

1. Goals in Settings, plus the evening check (steps only).
2. Quick-log actions on the notification and the Home card.
3. Active calories.
4. Phase multipliers.

Skipped: weekly summaries and streaks. Add them only if users ask.

## Risks

- **Notification fatigue:** at most one a day, off by default, silent when goals are met.
- **Late watch sync:** at 19:00 the watch may not have synced. Show "as of {last sync time}" and keep the tone soft.
- **Health framing:** this is encouragement, not treatment. No advice tied to symptoms beyond linking to the [[Health Content Library]].



