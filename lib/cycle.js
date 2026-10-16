import * as joda from 'js-joda'
import { getCycleLengthStats } from './cycle-length'
import { getTodayDateString } from './today'
import {
  getMensesForCycleStart,
  getOvulationEstimate,
  isProfileComplete,
  predictNextCycleLength,
  TYPICAL_LUTEAL_LENGTH,
} from './prediction'
import {
  buildTimedForecast,
  enumerateDateRange,
} from './prediction/forecast'
const LocalDate = joda.LocalDate
const DAYS = joda.ChronoUnit.DAYS

function getDeveloperCycleOverrides() {
  try {
    const { cycleOverridesObservable } = require('../local-storage')
    const overrides = cycleOverridesObservable.value
    if (overrides && overrides.enabled) return overrides
  } catch (err) {
    // local-storage may be unavailable in some test setups
  }
  return null
}

function isMlPredictionEnabled() {
  try {
    const { mlPredictionObservable } = require('../local-storage')
    return mlPredictionObservable.value !== false
  } catch (err) {
    return false
  }
}

function readStoredCycleProfile() {
  try {
    const { cycleProfileObservable } = require('../local-storage')
    return cycleProfileObservable.value || null
  } catch (err) {
    return null
  }
}

export default function config(opts) {
  let bleedingDaysSortedByDate
  let cycleStartsSortedByDate
  let cycleDaysSortedByDate
  let maxBreakInBleeding
  let maxCycleLength
  let minCyclesForPrediction
  let useMlPrediction
  let cycleProfile

  if (!opts) {
    // we only want to require (and run) the db module
    // when not running the tests
    bleedingDaysSortedByDate = require('../db').getBleedingDaysSortedByDate()
    cycleStartsSortedByDate = require('../db').getCycleStartsSortedByDate()
    cycleDaysSortedByDate = require('../db').getCycleDaysSortedByDate()
    const overrides = getDeveloperCycleOverrides()
    maxBreakInBleeding = overrides ? overrides.maxBreakInBleeding : 1
    maxCycleLength = overrides ? overrides.maxCycleLength : 99
    minCyclesForPrediction = overrides ? overrides.minCyclesForPrediction : 3
    useMlPrediction = isMlPredictionEnabled()
    cycleProfile = readStoredCycleProfile()
  } else {
    bleedingDaysSortedByDate = opts.bleedingDaysSortedByDate || []
    cycleStartsSortedByDate = opts.cycleStartsSortedByDate || []
    cycleDaysSortedByDate = opts.cycleDaysSortedByDate || []
    maxBreakInBleeding = opts.maxBreakInBleeding || 1
    maxCycleLength = opts.maxCycleLength || 99
    minCyclesForPrediction = opts.minCyclesForPrediction || 3
    useMlPrediction = opts.useMlPrediction || false
    cycleProfile = Object.prototype.hasOwnProperty.call(opts, 'profile')
      ? opts.profile
      : readStoredCycleProfile()
  }

  function getLastMensesStartForDay(targetDateString) {
    return cycleStartsSortedByDate.find(start => start.date <= targetDateString)
  }

  function getCycleDayNumber(targetDateString) {
    const lastMensesStart = getLastMensesStartForDay(targetDateString)
    if (!lastMensesStart) return null
    const targetDate = LocalDate.parse(targetDateString)
    const lastMensesLocalDate = LocalDate.parse(lastMensesStart.date)
    const diffInDays = lastMensesLocalDate.until(targetDate, DAYS)
    // take maxCycleLength into account (we don't display cycle day numbers higher than 99 at the moment)
    if (diffInDays >= maxCycleLength) return null
    // cycle starts at day 1
    return diffInDays + 1
  }

  function getPreviousCycle(dateString) {
    const cycleStart = getLastMensesStartForDay(dateString)
    if (!cycleStart) return null
    const i = cycleStartsSortedByDate.indexOf(cycleStart)
    const earlierCycleStart = cycleStartsSortedByDate[i + 1]
    if (!earlierCycleStart) return null
    return getCycleByStartDay(earlierCycleStart)
  }

  function getCyclesBefore(targetCycleStartDay) {
    const startFromHere = cycleStartsSortedByDate.findIndex(start => {
      return start.date < targetCycleStartDay.date
    })
    if (startFromHere < 0) return null
    return cycleStartsSortedByDate
      .slice(startFromHere)
      .map(start => getCycleByStartDay(start))
      // filter the ones exceeding maxCycleLength, those are null
      .filter(cycle => cycle)
  }

  function findIndexOfDay(day, daysSortedByDate) {
    return daysSortedByDate.findIndex(d => d.date === day.date)
  }

  function getNextCycleStartDay(startDay, cycleStartsSortedByDate) {
    const cycleStartIndex = findIndexOfDay(
      startDay,
      cycleStartsSortedByDate)
    return cycleStartsSortedByDate[cycleStartIndex - 1]
  }

  function getTodayDate() {
    return getTodayDateString()
  }

  function getCycleLength(startDate, endDate) {
    return LocalDate.parse(startDate)
      .until(LocalDate.parse(endDate), DAYS)
  }

  function isValidCycle(startDate, endDate) {
    return getCycleLength(startDate, endDate) <= maxCycleLength
  }

  function getCycleByStartDay(startDay, todayDate) {
    let cycleEndDate = todayDate || getTodayDate()
    let cycleEndIndex = 0

    const nextCycleStart = getNextCycleStartDay(
      startDay,
      cycleStartsSortedByDate
    )

    if (nextCycleStart) {
      const nextCycleIndex = findIndexOfDay(
        nextCycleStart,
        cycleDaysSortedByDate
      )
      cycleEndIndex = nextCycleIndex + 1
      cycleEndDate = nextCycleStart.date
    }

    if (isValidCycle(startDay.date, cycleEndDate)) {
      const cycleStartIndex = findIndexOfDay(
        startDay,
        cycleDaysSortedByDate
      )
      return cycleDaysSortedByDate
        .slice(cycleEndIndex, cycleStartIndex + 1)
    }

    return null
  }

  function getCycleForDay(dayOrDate, todayDate) {
    const dateString = typeof dayOrDate === 'string' ? dayOrDate : dayOrDate.date
    const cycleStart = getLastMensesStartForDay(dateString)
    if (!cycleStart) return null
    return getCycleByStartDay(cycleStart, todayDate)
  }

  function isMensesStart(cycleDay) {
    if (!cycleDay.bleeding || cycleDay.bleeding.exclude) return false
    if (noBleedingDayWithinThresholdBefore(cycleDay)) return true
    return false

    // checks that there are no relevant bleeding days before
    // the input cycleDay (returns boolean)
    function noBleedingDayWithinThresholdBefore(cycleDay) {
      const localDate = LocalDate.parse(cycleDay.date)
      const threshold = localDate.minusDays(maxBreakInBleeding + 1).toString()
      const bleedingDays = bleedingDaysSortedByDate
      const index = bleedingDays.findIndex(day => day.date === cycleDay.date)
      const candidates = bleedingDays.slice(index + 1)
      return !candidates.some(day => {
        return day.date >= threshold && !day.bleeding.exclude
      })
    }
  }

  // returns all bleeding days that belong to one menses directly following
  // the cycle day. used to set or clear new cycle starts when the target day
  // changes
  function getMensesDaysRightAfter(cycleDay) {
    const bleedingDays = bleedingDaysSortedByDate
      .filter(d => !d.bleeding.exclude)
      .reverse()
    const firstFollowingBleedingDayIndex = bleedingDays.findIndex(day => {
      return day.date > cycleDay.date
    })
    return recurse(cycleDay, firstFollowingBleedingDayIndex, [])

    // we look at the current bleeding day as well as the next, and decide
    // whether they belong to one menses. if they do, we collect them, once
    // they don't, we're done
    function recurse(day, nextIndex, mensesDays) {
      const next = bleedingDays[nextIndex]
      if (!next) return mensesDays
      if (!isWithinThreshold(day, next)) return mensesDays
      mensesDays.unshift(next)
      return recurse(next, nextIndex + 1, mensesDays)
    }

    // checks whether the two days belong to one menses episode
    function isWithinThreshold(bleedingDay, nextBleedingDay) {
      const localDate = LocalDate.parse(bleedingDay.date)
      const threshold = localDate.plusDays(maxBreakInBleeding + 1).toString()
      return nextBleedingDay.date <= threshold
    }
  }

  // completed cycles, newest first. each entry pairs a cycle's length with the
  // day it started, so cycle-level features stay aligned with cycle lengths.
  function getCompletedCycles() {
    return cycleStartsSortedByDate
      .map((cycleStart, i) => {
        const previousStart = cycleStartsSortedByDate[i + 1]
        if (!previousStart) return null
        const length = LocalDate.parse(previousStart.date)
          .until(LocalDate.parse(cycleStart.date), DAYS)
        if (!length || length > maxCycleLength) return null
        return { length, start: previousStart }
      })
      .filter(cycle => cycle)
  }

  function getAllCycleLengths() {
    return getCompletedCycles().map(cycle => cycle.length)
  }

  function getModelInput(cycleLengths) {
    return {
      cycleLengths,
      mensesPerCycle: getCompletedCycles().map((cycle) =>
        getMensesForCycleStart(
          cycle.start.date,
          bleedingDaysSortedByDate,
          maxBreakInBleeding
        )
      ),
      profile: cycleProfile,
    }
  }

  // uses the on-device gradient boosted model when it is enabled and has
  // enough history; returns null so callers fall back to the cycle mean
  function getModelCycleLength(cycleLengths) {
    if (!useMlPrediction) return null
    return predictNextCycleLength(getModelInput(cycleLengths))
  }

  function getPredictedLength(cycleLengths) {
    if (cycleLengths.length < minCyclesForPrediction) {
      return { predictedLength: null, lengthSource: null }
    }
    const modelCycleLength = getModelCycleLength(cycleLengths)
    const cycleInfo = getCycleLengthStats(cycleLengths.slice())
    if (modelCycleLength) {
      return { predictedLength: modelCycleLength, lengthSource: 'model' }
    }
    return {
      predictedLength: Math.round(cycleInfo.mean),
      lengthSource: 'average',
    }
  }

  function getPredictedMenses() {
    const cycleLengths = getAllCycleLengths()
    if (cycleLengths.length < minCyclesForPrediction) {
      return []
    }
    // getCycleLengthStats sorts its input, so predict before the order is lost
    const { predictedLength } = getPredictedLength(cycleLengths)
    const cycleInfo = getCycleLengthStats(cycleLengths.slice())
    const periodDistance = predictedLength || Math.round(cycleInfo.mean)
    let periodStartVariation
    if (cycleInfo.stdDeviation === null) {
      periodStartVariation = 2
    } else if (cycleInfo.stdDeviation < 1.5) { // threshold is chosen a little arbitrarily
      periodStartVariation = 1
    } else {
      periodStartVariation = 2
    }
    if (periodDistance - 5 < periodStartVariation) { // otherwise predictions overlap
      return []
    }
    const allMensesStarts = cycleStartsSortedByDate
    let lastStart = LocalDate.parse(allMensesStarts[0].date)
    const predictedMenses = []
    for (let i = 0; i < 3; i++) {
      lastStart = lastStart.plusDays(periodDistance)
      const nextPredictedDates = [lastStart.toString()]
      for (let j = 0; j < periodStartVariation; j++) {
        nextPredictedDates.push(lastStart.minusDays(j + 1).toString())
        nextPredictedDates.push(lastStart.plusDays(j + 1).toString())
      }
      nextPredictedDates.sort()
      predictedMenses.push(nextPredictedDates)
    }
    return predictedMenses
  }

  function getCycleForecast() {
    const today = getTodayDate()
    const cycleLengths = getAllCycleLengths()
    const predictedMenses = getPredictedMenses()
    const { predictedLength, lengthSource } = getPredictedLength(cycleLengths)
    const lastStart = cycleStartsSortedByDate[0] || null

    let ovulationDay = null
    let ovulationSource = null
    if (predictedLength) {
      if (useMlPrediction) {
        const estimate = getOvulationEstimate({
          ...getModelInput(cycleLengths),
          predictedLength,
        })
        if (estimate) {
          ovulationDay = estimate.day
          ovulationSource = estimate.source
        }
      } else {
        ovulationDay = Math.max(
          8,
          Math.min(predictedLength - 1, predictedLength - TYPICAL_LUTEAL_LENGTH)
        )
        ovulationSource = 'luteal'
      }
    }

    const timed = buildTimedForecast({
      todayDateString: today,
      cycleStartDate: lastStart ? lastStart.date : null,
      predictedLength,
      ovulationDay,
      predictedMenses,
    })

    return {
      today,
      cycleDayNumber: getCycleDayNumber(today),
      cycleStartDate: lastStart ? lastStart.date : null,
      predictedLength,
      lengthSource,
      predictedMenses,
      ovulationDay,
      ovulationSource,
      profileComplete: isProfileComplete(cycleProfile),
      hasEnoughHistory: cycleLengths.length >= minCyclesForPrediction,
      completedCycles: cycleLengths.length,
      ...timed,
    }
  }

  function getPredictedFertileDays() {
    const forecast = getCycleForecast()
    if (!forecast.fertileStartDate || !forecast.fertileEndDate) return []
    return enumerateDateRange(
      forecast.fertileStartDate,
      forecast.fertileEndDate
    )
  }

  return {
    getCycleDayNumber,
    getCycleForDay,
    getPreviousCycle,
    getCyclesBefore,
    getAllCycleLengths,
    getPredictedMenses,
    getCycleForecast,
    getPredictedFertileDays,
    isMensesStart,
    getMensesDaysRightAfter,
    getCycleByStartDay,
  }
}
