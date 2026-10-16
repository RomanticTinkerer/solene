import { compileBooster } from './xgboost'

/**
 * Boosters are a few hundred kilobytes of JSON each, so they are required
 * lazily and compiled at most once per app run.
 */
const SOURCES = {
  cycleLength: {
    booster: () => require('../../assets/models/cycle_length.json'),
    meta: () => require('../../assets/models/cycle_length_meta.json'),
  },
  ovulationDay: {
    booster: () => require('../../assets/models/ovulation_day.json'),
    meta: () => require('../../assets/models/ovulation_day_meta.json'),
  },
}

const cache = {}

export function loadModel(name) {
  if (cache[name]) return cache[name]

  const source = SOURCES[name]
  if (!source) throw new Error(`Unknown prediction model: ${name}`)

  const meta = source.meta()
  cache[name] = {
    booster: compileBooster(source.booster()),
    clamps: meta.clamps || {},
    featureDefaults: meta.feature_defaults || {},
    featureNames: meta.feature_names || [],
    metrics: meta.metrics || {},
    target: meta.target,
  }
  return cache[name]
}

export function clearModelCache() {
  Object.keys(cache).forEach((key) => delete cache[key])
}
