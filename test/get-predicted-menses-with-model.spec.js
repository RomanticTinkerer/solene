import chai from 'chai'
import dirtyChai from 'dirty-chai'
import { LocalDate, ChronoUnit } from 'js-joda'

import cycleModule from '../lib/cycle'

const { expect } = chai
chai.use(dirtyChai)

/**
 * Builds cycle days for the given cycle lengths, newest cycle first,
 * with a two day menses at the start of every cycle.
 */
function makeHistory(cycleLengths, lastStart = '2021-06-01') {
  const starts = [LocalDate.parse(lastStart)]
  cycleLengths.forEach((length) => {
    starts.push(starts[starts.length - 1].minusDays(length))
  })

  const cycleDaysSortedByDate = []
  starts.forEach((start) => {
    cycleDaysSortedByDate.push({
      date: start.plusDays(1).toString(),
      bleeding: { value: 1, exclude: false },
    })
    cycleDaysSortedByDate.push({
      date: start.toString(),
      bleeding: { value: 2, exclude: false },
    })
  })
  cycleDaysSortedByDate.sort((a, b) => (a.date < b.date ? 1 : -1))

  const startDates = starts.map((s) => s.toString())
  return {
    cycleDaysSortedByDate,
    bleedingDaysSortedByDate: cycleDaysSortedByDate,
    cycleStartsSortedByDate: cycleDaysSortedByDate.filter((d) =>
      startDates.includes(d.date)
    ),
    maxCycleLength: 99,
    minCyclesForPrediction: 3,
  }
}

function getPredictedDistance(opts) {
  const { getPredictedMenses } = cycleModule(opts)
  const prediction = getPredictedMenses()
  if (!prediction.length) return null
  const middleOfFirst = prediction[0][Math.floor(prediction[0].length / 2)]
  const lastStart = opts.cycleStartsSortedByDate[0].date
  return LocalDate.parse(lastStart).until(
    LocalDate.parse(middleOfFirst),
    ChronoUnit.DAYS
  )
}

const IRREGULAR = [26, 34, 29, 40, 25, 31]

describe('getPredictedMenses with the on-device model', () => {
  it('is off by default so the statistical prediction is unchanged', () => {
    expect(getPredictedDistance(makeHistory([28, 28, 28, 28]))).to.eql(28)
    expect(getPredictedDistance(makeHistory(IRREGULAR))).to.eql(31)
  })

  it('leaves regular cycles on the statistical prediction', () => {
    const history = makeHistory([28, 28, 28, 28])
    expect(getPredictedDistance({ ...history, useMlPrediction: true })).to.eql(
      getPredictedDistance(history)
    )
  })

  it('uses the model for irregular cycles', () => {
    const history = makeHistory(IRREGULAR)
    const statistical = getPredictedDistance(history)
    const model = getPredictedDistance({ ...history, useMlPrediction: true })

    expect(model).to.be.a('number')
    expect(model).to.not.eql(statistical)
    expect(model).to.be.within(Math.min(...IRREGULAR), Math.max(...IRREGULAR))
  })

  it('follows short and long irregular histories in the right direction', () => {
    const short = getPredictedDistance({
      ...makeHistory([21, 29, 22, 30, 23]),
      useMlPrediction: true,
    })
    const long = getPredictedDistance({
      ...makeHistory([36, 44, 37, 45, 38]),
      useMlPrediction: true,
    })
    expect(short).to.be.below(long)
  })

  it('still returns nothing below minCyclesForPrediction', () => {
    const history = makeHistory([28])
    const { getPredictedMenses } = cycleModule({
      ...history,
      useMlPrediction: true,
    })
    expect(getPredictedMenses()).to.eql([])
  })

  it('produces three future predictions like the statistical path', () => {
    const { getPredictedMenses } = cycleModule({
      ...makeHistory(IRREGULAR),
      useMlPrediction: true,
    })
    expect(getPredictedMenses()).to.have.length(3)
  })

  it('does not let the model reorder the cycle lengths', () => {
    // getCycleLengthStats sorts its input in place; the model must see the
    // history newest-first regardless of the order the stats leave it in
    const history = makeHistory([26, 33, 27, 32])
    const { getAllCycleLengths, getPredictedMenses } = cycleModule({
      ...history,
      useMlPrediction: true,
    })
    getPredictedMenses()
    expect(getAllCycleLengths()).to.eql([26, 33, 27, 32])
  })

  it('exposes a cycle forecast with ovulation and a fertile window', () => {
    const { getCycleForecast, getPredictedFertileDays } = cycleModule({
      ...makeHistory(IRREGULAR),
      useMlPrediction: true,
    })
    const forecast = getCycleForecast()
    expect(forecast.hasEnoughHistory).to.be.true()
    expect(forecast.predictedLength).to.be.within(25, 40)
    expect(forecast.ovulationDay).to.be.within(8, 40)
    expect(forecast.fertileStartDate).to.be.a('string')
    expect(forecast.fertileEndDate).to.be.a('string')
    expect(getPredictedFertileDays().length).to.be.at.least(2)
  })

  it('accepts a body profile without throwing', () => {
    const distance = getPredictedDistance({
      ...makeHistory(IRREGULAR),
      useMlPrediction: true,
      profile: { age: 40, heightCm: 170, weightKg: 80 },
    })
    expect(distance).to.be.a('number')
  })
})
