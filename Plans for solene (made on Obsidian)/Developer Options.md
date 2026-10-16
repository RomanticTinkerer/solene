Developer Options.

Developer Options enabled by clicking the "Settings" tab 7 times to enable in the settings tab. Options include: Pregnancy Mode, Complete Cycle modification, calendric change, and changes usually not possible in the main app

---

# Developed plan (October 2026)

## Unlock

**Recommendation:** count 7 taps on the **Version** row (Settings → About), not on the bottom-bar Settings tab.

- Tapping the tab is also navigation, so taps are easy to miss or to trigger by accident.
- Android itself uses 7 taps on the build number, so the gesture is familiar.

Behaviour:

- From the 4th tap, show a short countdown ("3 taps away from developer options").
- Once unlocked, a "Developer options" section appears at the bottom of Settings with an off switch.
- The flag is one boolean in DataStore. No feature-flag framework.

## Options

### 1. Override today ("calendric change")

- Pick a date, and the whole app (calendar, predictions, Home, Stats) behaves as if it is that day. This makes every other feature testable without waiting weeks for a real cycle.
- **Cost:** about 25 direct calls to `LocalDate.now()` / `Instant.now()` across 16 files must go through one injected `java.time.Clock`. The override swaps the clock, and the change also makes the code easier to unit test.
- **Reminders:** paused while the override is on, so real alarms never fire for a fake date.
- A banner on every screen ("Simulated date: 14 Nov 2026") prevents confusion.

### 2. Pregnancy mode

A toggle for pregnancy mode while it's under construction ([[Pregnancy Tracking (Detailed)]]). It moves to normal Settings when it ships.

### 3. Cycle editing ("complete cycle modification")

Normal logging already edits any day. Developer options add what normal logging can't do:

- Edit the onboarding data: last period date and prior cycle lengths.
- Mark or unmark a day as a cycle start manually, overriding the automatic detection from bleeding. drip has this concept (`lib/set-new-cycle-start.js`).
- Exclude a bleeding day from cycle detection, e.g. bleeding that wasn't a period. drip supports `exclude` on bleeding values.

### 4. Prediction internals (read-only)

Shows, for the current cycle:

- the personal mean and the model output;
- the model's RMSE and the features used;
- which path was chosen, and why.

This helps explain odd predictions ([[Personalized Cycle Tracking and Prediction]]).

### 5. Test notifications

"Send daily reminder now" and "Send period reminder now". These call the existing `ReminderNotifications` functions directly.

### 6. Demo data and reset

- **Load demo data:** only allowed when the app has no data. Otherwise the user must export first, so demo cycles never mix with real ones.
- **Reset onboarding.**
- **Wipe all data.**

## Safety

Developer options can damage real cycle data. Every destructive action:

1. asks for confirmation that names what will be lost;
2. offers an export first, using the existing export in Settings.

## Not included

- A remote feature-flag service, or flags for every feature. One boolean per option is enough.
- Hiding developer options from release builds. The project is open source, so there's nothing to hide; the 7 taps just keep them out of the way.

## Milestones

1. Unlock gesture, section shell, test notifications. Small; proves the plumbing.
2. Injected `Clock` and the today override. Biggest item, and the one that unblocks testing everything else.
3. Prediction internals.
4. Cycle editing.
5. Pregnancy mode toggle, when that work starts.
