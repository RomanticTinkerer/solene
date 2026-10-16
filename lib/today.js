import { LocalDate } from 'js-joda'

/**
 * `local-storage` pulls in react-native, which is not available under mocha,
 * so it is required lazily the same way `lib/cycle` does it.
 */
function getCalendricOverride() {
  try {
    const { calendricOverrideObservable } = require('../local-storage')
    return calendricOverrideObservable.value
  } catch (err) {
    return null
  }
}

export function getTodayLocalDate() {
  const override = getCalendricOverride()
  if (override && override.enabled && override.date) {
    try {
      return LocalDate.parse(override.date)
    } catch (err) {
      return LocalDate.now()
    }
  }
  return LocalDate.now()
}

export function getTodayDateString() {
  return getTodayLocalDate().toString()
}
