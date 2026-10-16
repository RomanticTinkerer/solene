import { ChronoUnit, LocalDate } from 'js-joda'

import {
  FERTILE_DAYS_AFTER,
  FERTILE_DAYS_BEFORE,
} from './constants'

export function buildFertileWindow(ovulationDay, cycleLength) {
  if (typeof ovulationDay !== 'number' || typeof cycleLength !== 'number') {
    return null
  }
  const start = Math.max(1, ovulationDay - FERTILE_DAYS_BEFORE)
  const end = Math.min(cycleLength, ovulationDay + FERTILE_DAYS_AFTER)
  if (start > end) return null
  return { start, end, ovulationDay }
}

export function dateForCycleDay(cycleStartDate, cycleDay) {
  return LocalDate.parse(cycleStartDate)
    .plusDays(cycleDay - 1)
    .toString()
}

export function predictedPeriodStart(predictedMenses) {
  if (!predictedMenses || !predictedMenses.length) return null
  const first = predictedMenses[0]
  if (!first || !first.length) return null
  return first[Math.floor(first.length / 2)]
}

export function enumerateDateRange(startDate, endDate) {
  const dates = []
  let cursor = LocalDate.parse(startDate)
  const end = LocalDate.parse(endDate)
  while (!cursor.isAfter(end)) {
    dates.push(cursor.toString())
    cursor = cursor.plusDays(1)
  }
  return dates
}

export function daysUntil(fromDateString, toDateString) {
  return LocalDate.parse(fromDateString).until(
    LocalDate.parse(toDateString),
    ChronoUnit.DAYS
  )
}

export function isProfileComplete(profile) {
  if (!profile) return false
  return (
    typeof profile.age === 'number' &&
    typeof profile.heightCm === 'number' &&
    typeof profile.weightKg === 'number'
  )
}

/**
 * Dates for the fertile window of the current cycle, or the next one if
 * today's date is already past this cycle's window.
 */
export function buildTimedForecast({
  todayDateString,
  cycleStartDate,
  predictedLength,
  ovulationDay,
  predictedMenses,
} = {}) {
  const empty = {
    ovulationDate: null,
    fertileStartDate: null,
    fertileEndDate: null,
    windowIsNextCycle: false,
  }

  if (!cycleStartDate || !ovulationDay || !predictedLength || !todayDateString) {
    return empty
  }

  const window = buildFertileWindow(ovulationDay, predictedLength)
  if (!window) return empty

  const today = LocalDate.parse(todayDateString)
  let startDate = LocalDate.parse(cycleStartDate)
  let windowIsNextCycle = false
  const currentEnd = startDate.plusDays(window.end - 1)

  if (today.isAfter(currentEnd)) {
    const nextStart = predictedPeriodStart(predictedMenses)
    if (nextStart) {
      startDate = LocalDate.parse(nextStart)
      windowIsNextCycle = true
    }
  }

  return {
    ovulationDate: startDate.plusDays(window.ovulationDay - 1).toString(),
    fertileStartDate: startDate.plusDays(window.start - 1).toString(),
    fertileEndDate: startDate.plusDays(window.end - 1).toString(),
    windowIsNextCycle,
  }
}
