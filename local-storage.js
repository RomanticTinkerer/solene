import { AsyncStorage } from 'react-native'
import Observable from 'obv'
import { TEMP_SCALE_MIN, TEMP_SCALE_MAX, TEMP_SCALE_UNITS } from './config'

export const scaleObservable = Observable()
setObvWithInitValue('tempScale',
  scaleObservable, { min: TEMP_SCALE_MIN, max: TEMP_SCALE_MAX })

export const unitObservable = Observable()
unitObservable.set(TEMP_SCALE_UNITS)
scaleObservable((scale) => {
  const scaleRange = scale.max - scale.min
  if (scaleRange <= 1.5) {
    unitObservable.set(0.1)
  } else {
    unitObservable.set(0.5)
  }
})

export async function saveTempScale(scale) {
  await AsyncStorage.setItem('tempScale', JSON.stringify(scale))
  scaleObservable.set(scale)
}

export const tempReminderObservable = Observable()
setObvWithInitValue('tempReminder', tempReminderObservable, {
  enabled: false
})

export async function saveTempReminder(reminder) {
  await AsyncStorage.setItem('tempReminder', JSON.stringify(reminder))
  tempReminderObservable.set(reminder)
}

export const periodReminderObservable = Observable()
setObvWithInitValue('periodReminder', periodReminderObservable, {
  enabled: false
})

export async function savePeriodReminder(reminder) {
  await AsyncStorage.setItem('periodReminder', JSON.stringify(reminder))
  periodReminderObservable.set(reminder)
}

export const useCervixObservable = Observable()
setObvWithInitValue('useCervix', useCervixObservable, false)

export async function saveUseCervix(bool) {
  await AsyncStorage.setItem('useCervix', JSON.stringify(bool))
  useCervixObservable.set(bool)
}

export const hasEncryptionObservable = Observable()
setObvWithInitValue('hasEncryption', hasEncryptionObservable, false)

export async function saveEncryptionFlag(bool) {
  await AsyncStorage.setItem('hasEncryption', JSON.stringify(bool))
  hasEncryptionObservable.set(bool)
}


export async function getLicenseFlag() {
  return AsyncStorage.getItem('agreedToLicense')
}

export async function saveLicenseFlag() {
  await AsyncStorage.setItem('agreedToLicense', JSON.stringify(true))
}

export async function getChartFlag() {
  const isFirstChartView = await AsyncStorage.getItem('isFirstChartView')
  return isFirstChartView === null ? 'true' : isFirstChartView
}

export async function setChartFlag() {
  await AsyncStorage.setItem('isFirstChartView', JSON.stringify(false))
}

export const developerOptionsUnlockedObservable = Observable()
setObvWithInitValue(
  'developerOptionsUnlocked',
  developerOptionsUnlockedObservable,
  false
)

export async function saveDeveloperOptionsUnlocked(bool) {
  await AsyncStorage.setItem('developerOptionsUnlocked', JSON.stringify(bool))
  developerOptionsUnlockedObservable.set(bool)
}

export const pregnancyModeObservable = Observable()
setObvWithInitValue('pregnancyMode', pregnancyModeObservable, false)

export async function savePregnancyMode(bool) {
  await AsyncStorage.setItem('pregnancyMode', JSON.stringify(bool))
  pregnancyModeObservable.set(bool)
}

export const cycleOverridesObservable = Observable()
setObvWithInitValue('cycleOverrides', cycleOverridesObservable, {
  enabled: false,
  maxCycleLength: 99,
  minCyclesForPrediction: 3,
  maxBreakInBleeding: 1,
})

export async function saveCycleOverrides(overrides) {
  await AsyncStorage.setItem('cycleOverrides', JSON.stringify(overrides))
  cycleOverridesObservable.set(overrides)
}

export const mlPredictionObservable = Observable()
setObvWithInitValue('mlPrediction', mlPredictionObservable, true)

export async function saveMlPrediction(bool) {
  await AsyncStorage.setItem('mlPrediction', JSON.stringify(bool))
  mlPredictionObservable.set(bool)
}

export const cycleProfileObservable = Observable()
setObvWithInitValue('cycleProfile', cycleProfileObservable, {
  age: null,
  heightCm: null,
  weightKg: null,
})

export async function saveCycleProfile(profile) {
  await AsyncStorage.setItem('cycleProfile', JSON.stringify(profile))
  cycleProfileObservable.set(profile)
}

export const calendricOverrideObservable = Observable()
setObvWithInitValue('calendricOverride', calendricOverrideObservable, {
  enabled: false,
  date: null,
})

export async function saveCalendricOverride(override) {
  await AsyncStorage.setItem('calendricOverride', JSON.stringify(override))
  calendricOverrideObservable.set(override)
}

async function setObvWithInitValue(key, obv, defaultValue) {
  const result = await AsyncStorage.getItem(key)
  let value
  if (result) {
    value = JSON.parse(result)
  } else {
    value = defaultValue
  }
  obv.set(value)
}