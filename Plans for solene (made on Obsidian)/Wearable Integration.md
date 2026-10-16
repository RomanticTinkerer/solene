*Extract data from Google Fit, Da Fit or any other fitness trackware of proprietary wearables in order to give recommendations based on body temps, heart rate, spO2 and blood pressure*

---

# Developed plan (October 2026)

## Reality check on data sources

- **Google Fit:** Google has deprecated the Fit APIs in favour of Health Connect. Don't build on them.
- **Da Fit and other vendor apps:** no public API. Their data reaches Solene only if the vendor app writes to Health Connect. Health Connect's own settings show which apps write which data, so check each app there.
- **Watches whose app doesn't write to Health Connect:** Gadgetbridge (FOSS, replaces vendor apps for many cheap watches, including some Moyoung / Da Fit devices) is the open route. Whether a given Gadgetbridge version writes to Health Connect has to be verified before promising it to users.

Health Connect is therefore the only integration point, and Solene already uses it.

## What exists today

`HealthConnectRepository` reads:

- From the last 24 hours, latest value only: heart rate, resting heart rate, SpO2, body temperature, respiratory rate and blood pressure.
- Steps today and last night's sleep.

The You tab shows these on a vitals card. Nothing is stored, charted, or used by predictions.

## v1 scope

### 1. Daily history

- One repository function returns a per-day series for the last N days:
  - resting heart rate;
  - night temperature;
  - sleep duration;
  - steps.
- Use Health Connect's `aggregate` calls where possible. They de-duplicate overlapping sources (phone + watch), as the steps code already does.
- Health Connect stays the store. Solene re-reads on demand instead of copying health data into its own storage.
- Day boundary: overnight readings belong to the wake-up date. This matches the morning-temperature convention the symptothermal rules use.

### 2. Temperature into the day log

- If Health Connect has a `BasalBodyTemperatureRecord` for a day with no logged temperature, prefill the day log's temperature field. Label it "from Health Connect"; the user saves to accept it.
- Never prefill from wrist skin temperature. Skin temperature is not basal body temperature, and mixing measurement methods breaks the temperature-shift rule ([[Personalized Cycle Tracking and Prediction]]).
- Skin temperature can still be shown as a chart, as a relative change from the person's own baseline, without feeding fertility rules.

### 3. Cycle overlay chart

- Plot resting heart rate and night temperature against cycle day on Stats or You.
- A rise in the luteal phase is a known pattern for many people. Show it as information only: "your resting heart rate was 3 bpm higher in the second half of your cycle".
- Always compare against the person's own baseline, never against population thresholds.

### 4. Import periods from other trackers

- Read `MenstruationFlowRecord` / `MenstruationPeriodRecord` once, on request. Someone switching from another tracker that wrote to Health Connect gets their history without CSV juggling.
- Imported days go through the normal `saveCycleDay` path.

### 5. Write periods back (opt-in, off by default)

- Write logged bleeding as `MenstruationFlowRecord` so other apps can see it.
- Off by default: cycle data is sensitive, and many users won't want it outside Solene.

## Explicitly out of scope: blood pressure and SpO2 recommendations

The original idea includes recommendations from SpO2 and blood pressure. Skip that:

- There is no cycle-specific guidance for these that is safe to automate.
- Flagging "abnormal" readings turns the app into a medical device.

Keep displaying the values as they are. Revisit only with a clinician involved.

## Permissions

Request each permission when the feature that needs it is turned on, not all at first launch.

| Permission | Needed for |
|---|---|
| `READ_BASAL_BODY_TEMPERATURE` | Temperature prefill |
| `READ_MENSTRUATION` | Import |
| `WRITE_MENSTRUATION` | Write-back (opt-in) |
| `READ_ACTIVE_CALORIES_BURNED` | [[Daily well-being plan]] |
| `READ_HEALTH_DATA_IN_BACKGROUND` | [[Daily well-being plan]] (reading from a notification alarm) |

The manifest already has the permission-rationale activity Health Connect requires.

## Milestones

1. Daily-series function plus unit tests for the day-boundary rule.
2. Temperature prefill in the day log.
3. Resting heart rate / temperature cycle chart.
4. One-time period import.
5. Opt-in write-back.

## Risks

- **Gaps:** the watch wasn't worn or synced late. Show "no data" rather than interpolating, and show the time of the last sync.
- **Duplicate sources:** prefer aggregates; for raw records, take a single source per day.
- **Time zones:** use the record's own zone offset when assigning days, not the phone's current zone.
