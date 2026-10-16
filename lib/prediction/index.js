import { loadModel } from './models'
import { predict } from './xgboost'
import { applyFeatureDefaults, buildCycleFeatures } from './features'
import {
  MIN_CYCLES_FOR_MODEL,
  TYPICAL_LUTEAL_LENGTH,
} from './constants'

export { buildCycleFeatures, getMensesForCycleStart } from './features'
export {
  MIN_CYCLES_FOR_MODEL,
  TYPICAL_LUTEAL_LENGTH,
  FERTILE_DAYS_BEFORE,
  FERTILE_DAYS_AFTER,
} from './constants'
export {
  buildFertileWindow,
  buildTimedForecast,
  isProfileComplete,
  predictedPeriodStart,
} from './forecast'

/* The models were trained on clients with several logged cycles; below this
 * the statistical mean is at least as trustworthy. */

function clamp(value, min, max) {
  let result = value
  if (typeof min === 'number') result = Math.max(min, result)
  if (typeof max === 'number') result = Math.min(max, result)
  return result
}

function runModel(name, features) {
  const model = loadModel(name)
  const vector = applyFeatureDefaults(
    features,
    model.featureNames,
    model.featureDefaults
  )
  const raw = predict(model.booster, vector)
  return clamp(raw, model.clamps.min, model.clamps.max)
}

function hasEnoughHistory(cycleLengths) {
  return cycleLengths.length >= MIN_CYCLES_FOR_MODEL
}

function mean(values) {
  return values.reduce((total, value) => total + value, 0) / values.length
}

function standardDeviation(values) {
  if (values.length < 2) return 0
  const avg = mean(values)
  const sumOfSquares = values.reduce(
    (total, v) => total + Math.pow(v - avg, 2),
    0
  )
  return Math.sqrt(sumOfSquares / (values.length - 1))
}

function typicalLength(cycleLengths, predictedLength) {
  if (typeof predictedLength === 'number') return predictedLength
  if (!cycleLengths.length) return null
  return Math.round(mean(cycleLengths))
}

/**
 * Raw model output for the length of the cycle that is currently running.
 * Returns null when there is too little history.
 */
export function runCycleLengthModel(input) {
  const { cycleLengths = [] } = input || {}
  if (!hasEnoughHistory(cycleLengths)) return null

  try {
    return Math.round(runModel('cycleLength', buildCycleFeatures(input)))
  } catch (err) {
    return null
  }
}

function lutealOvulationDay(cycleLength) {
  return clamp(cycleLength - TYPICAL_LUTEAL_LENGTH, 8, cycleLength - 1)
}

/**
 * Ovulation day of the running cycle, plus whether it came from the model
 * or from the typical 14-day luteal phase.
 */
export function getOvulationEstimate(input) {
  const { cycleLengths = [], predictedLength } = input || {}
  if (!hasEnoughHistory(cycleLengths)) return null

  const length = typicalLength(cycleLengths, predictedLength)
  if (!length) return null

  const luteal = {
    day: lutealOvulationDay(length),
    source: 'luteal',
  }

  let modelError
  try {
    modelError = loadModel('ovulationDay').metrics.rmse
  } catch (err) {
    return luteal
  }

  if (standardDeviation(cycleLengths) <= modelError) return luteal

  try {
    const modeled = Math.round(
      runModel('ovulationDay', buildCycleFeatures(input))
    )
    return {
      day: clamp(modeled, 8, Math.min(40, length - 1)),
      source: 'model',
    }
  } catch (err) {
    return luteal
  }
}

/** Raw ovulation day of the cycle that is running, or null. */
export function predictOvulationDay(input) {
  const estimate = getOvulationEstimate(input)
  return estimate ? estimate.day : null
}

/**
 * Cycle length prediction for the app, or null when the user's own average
 * should be used instead.
 *
 * The model only saw the population's typical cycles, so it pulls unusual
 * histories back towards ~29 days. Two guards keep that from making the app's
 * predictions worse than the average it already had:
 *
 * 1. If the user's cycles vary by less than the model's own held-out error,
 *    their average is the more precise estimate, so the model is skipped.
 * 2. Otherwise the model may add information, but its answer is held inside
 *    the range of cycle lengths the user has actually recorded.
 */
export function predictNextCycleLength(input) {
  const { cycleLengths = [] } = input || {}
  if (!hasEnoughHistory(cycleLengths)) return null

  let modelError
  try {
    modelError = loadModel('cycleLength').metrics.rmse
  } catch (err) {
    return null
  }

  if (standardDeviation(cycleLengths) <= modelError) return null

  const predicted = runCycleLengthModel(input)
  if (predicted === null) return null

  return clamp(
    predicted,
    Math.min(...cycleLengths),
    Math.max(...cycleLengths)
  )
}
