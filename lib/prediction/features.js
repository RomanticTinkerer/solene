import { LocalDate, ChronoUnit } from 'js-joda'

/**
 * The NFP training set scores each menses day from 1 (light) to 3 (heavy),
 * while drip records four levels. Spotting and light both map to 1 so the
 * scores stay on the scale the models were trained on.
 */
const BLEEDING_VALUE_TO_MENSES_SCORE = [1, 1, 2, 3]

const MENSES_SCORE_FEATURES = [
  'MensesScoreDayOne',
  'MensesScoreDayTwo',
  'MensesScoreDayThree',
  'MensesScoreDayFour',
  'MensesScoreDayFive',
  'MensesScoreDaySix',
  'MensesScoreDaySeven',
]

const POUNDS_PER_KG = 2.20462
const INCHES_PER_CM = 0.393701
/* BMI from imperial units, matching the dataset's Height (in) / Weight (lb). */
const IMPERIAL_BMI_FACTOR = 703

function mean(values) {
  if (!values.length) return null
  return values.reduce((total, value) => total + value, 0) / values.length
}

function daysBetween(fromDateString, toDateString) {
  return LocalDate.parse(fromDateString).until(
    LocalDate.parse(toDateString),
    ChronoUnit.DAYS
  )
}

function toMensesScore(bleedingValue) {
  const score = BLEEDING_VALUE_TO_MENSES_SCORE[bleedingValue]
  return score === undefined ? null : score
}

/**
 * Collects the run of bleeding days that opens a cycle. `bleedingDays` is
 * sorted newest first, like everywhere else in the app.
 */
export function getMensesForCycleStart(
  startDate,
  bleedingDays,
  maxBreakInBleeding
) {
  const relevant = bleedingDays
    .filter((day) => day.bleeding && !day.bleeding.exclude)
    .filter((day) => day.date >= startDate)
    .sort((a, b) => (a.date < b.date ? -1 : 1))

  const menses = []
  let previousDate = null
  for (const day of relevant) {
    if (previousDate !== null) {
      const gap = daysBetween(previousDate, day.date)
      if (gap > maxBreakInBleeding + 1) break
    } else if (day.date !== startDate) {
      break
    }
    menses.push(day)
    previousDate = day.date
  }
  return menses
}

function getMensesStats(menses) {
  const scores = menses
    .map((day) => toMensesScore(day.bleeding.value))
    .filter((score) => score !== null)

  return {
    length: menses.length || null,
    scores,
    totalScore: scores.length
      ? scores.reduce((total, score) => total + score, 0)
      : null,
  }
}

function getProfileFeatures(profile) {
  if (!profile) return {}

  const features = {}
  if (typeof profile.age === 'number') features.Age = profile.age

  const heightIn =
    typeof profile.heightCm === 'number'
      ? profile.heightCm * INCHES_PER_CM
      : null
  const weightLb =
    typeof profile.weightKg === 'number' ? profile.weightKg * POUNDS_PER_KG : null

  if (heightIn !== null) features.Height = heightIn
  if (weightLb !== null) features.Weight = weightLb
  if (heightIn !== null && weightLb !== null && heightIn > 0) {
    features.BMI = (IMPERIAL_BMI_FACTOR * weightLb) / (heightIn * heightIn)
  }
  return features
}

/**
 * Builds the model input from what the app actually knows. Anything that
 * cannot be derived is left out here and filled from the model's trained
 * medians by `applyFeatureDefaults`.
 *
 * @param cycleLengths lengths of completed cycles, newest first
 * @param mensesPerCycle menses day arrays per completed cycle, newest first
 * @param profile optional { age, heightCm, weightKg }
 */
export function buildCycleFeatures({
  cycleLengths = [],
  mensesPerCycle = [],
  profile = null,
} = {}) {
  const features = {}

  if (cycleLengths.length) {
    features.LengthofCycle = cycleLengths[0]
    features.MeanCycleLength = mean(cycleLengths)
  }

  const stats = mensesPerCycle.map(getMensesStats)
  const lengths = stats.map((s) => s.length).filter((l) => l !== null)
  const totals = stats.map((s) => s.totalScore).filter((t) => t !== null)

  if (lengths.length) {
    features.LengthofMenses = lengths[0]
    features.MeanMensesLength = mean(lengths)
  }
  if (totals.length) {
    features.TotalMensesScore = totals[0]
    features.MeanBleedingIntensity = mean(totals)
  }
  if (stats.length) {
    MENSES_SCORE_FEATURES.forEach((name, i) => {
      const score = stats[0].scores[i]
      if (score !== undefined) features[name] = score
    })
  }

  return { ...features, ...getProfileFeatures(profile) }
}

export function applyFeatureDefaults(features, featureNames, defaults) {
  const complete = {}
  featureNames.forEach((name) => {
    const value = features[name]
    if (typeof value === 'number' && !Number.isNaN(value)) {
      complete[name] = value
      return
    }
    const fallback = defaults[name]
    complete[name] = typeof fallback === 'number' ? fallback : NaN
  })
  return complete
}
