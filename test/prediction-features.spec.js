import chai from 'chai'
import dirtyChai from 'dirty-chai'

import {
  applyFeatureDefaults,
  buildCycleFeatures,
  getMensesForCycleStart,
} from '../lib/prediction/features'
import {
  getOvulationEstimate,
  predictNextCycleLength,
  predictOvulationDay,
  runCycleLengthModel,
  MIN_CYCLES_FOR_MODEL,
} from '../lib/prediction'

const expect = chai.expect
chai.use(dirtyChai)

const bleedingDay = (date, value) => ({
  date,
  bleeding: { value, exclude: false },
})

describe('prediction features', () => {
  describe('getMensesForCycleStart', () => {
    it('collects consecutive bleeding days from the cycle start', () => {
      const bleedingDays = [
        bleedingDay('2021-03-04', 0),
        bleedingDay('2021-03-03', 1),
        bleedingDay('2021-03-02', 3),
        bleedingDay('2021-03-01', 2),
      ]
      const menses = getMensesForCycleStart('2021-03-01', bleedingDays, 1)
      expect(menses.map((d) => d.date)).to.eql([
        '2021-03-01',
        '2021-03-02',
        '2021-03-03',
        '2021-03-04',
      ])
    })

    it('bridges a gap within maxBreakInBleeding', () => {
      const bleedingDays = [
        bleedingDay('2021-03-03', 1),
        bleedingDay('2021-03-01', 2),
      ]
      const menses = getMensesForCycleStart('2021-03-01', bleedingDays, 1)
      expect(menses).to.have.length(2)
    })

    it('stops at a gap larger than maxBreakInBleeding', () => {
      const bleedingDays = [
        bleedingDay('2021-03-10', 1),
        bleedingDay('2021-03-01', 2),
      ]
      const menses = getMensesForCycleStart('2021-03-01', bleedingDays, 1)
      expect(menses.map((d) => d.date)).to.eql(['2021-03-01'])
    })

    it('ignores excluded bleeding days', () => {
      const bleedingDays = [
        { date: '2021-03-02', bleeding: { value: 1, exclude: true } },
        bleedingDay('2021-03-01', 2),
      ]
      const menses = getMensesForCycleStart('2021-03-01', bleedingDays, 1)
      expect(menses.map((d) => d.date)).to.eql(['2021-03-01'])
    })

    it('returns nothing when the cycle start itself has no bleeding', () => {
      const bleedingDays = [bleedingDay('2021-03-05', 2)]
      expect(getMensesForCycleStart('2021-03-01', bleedingDays, 1)).to.eql([])
    })
  })

  describe('buildCycleFeatures', () => {
    it('derives cycle length features newest first', () => {
      const features = buildCycleFeatures({ cycleLengths: [30, 28, 26] })
      expect(features.LengthofCycle).to.eql(30)
      expect(features.MeanCycleLength).to.eql(28)
    })

    it('maps drip bleeding values onto the 1-3 menses score scale', () => {
      const features = buildCycleFeatures({
        cycleLengths: [28],
        mensesPerCycle: [
          [
            bleedingDay('2021-03-01', 0),
            bleedingDay('2021-03-02', 1),
            bleedingDay('2021-03-03', 2),
            bleedingDay('2021-03-04', 3),
          ],
        ],
      })
      expect(features.MensesScoreDayOne).to.eql(1)
      expect(features.MensesScoreDayTwo).to.eql(1)
      expect(features.MensesScoreDayThree).to.eql(2)
      expect(features.MensesScoreDayFour).to.eql(3)
      expect(features.LengthofMenses).to.eql(4)
      expect(features.TotalMensesScore).to.eql(7)
    })

    it('only records scores for the days that exist', () => {
      const features = buildCycleFeatures({
        cycleLengths: [28],
        mensesPerCycle: [[bleedingDay('2021-03-01', 2)]],
      })
      expect(features.MensesScoreDayOne).to.eql(2)
      expect(features).to.not.have.property('MensesScoreDayTwo')
    })

    it('averages menses length and total score across cycles', () => {
      const features = buildCycleFeatures({
        cycleLengths: [28, 30],
        mensesPerCycle: [
          [bleedingDay('2021-03-01', 3), bleedingDay('2021-03-02', 3)],
          [bleedingDay('2021-02-01', 1)],
        ],
      })
      expect(features.MeanMensesLength).to.eql(1.5)
      expect(features.MeanBleedingIntensity).to.eql(3.5)
    })

    it('converts a metric profile into the dataset imperial units', () => {
      const features = buildCycleFeatures({
        cycleLengths: [28],
        profile: { age: 30, heightCm: 165, weightKg: 60 },
      })
      expect(features.Age).to.eql(30)
      expect(features.Height).to.be.closeTo(64.96, 0.01)
      expect(features.Weight).to.be.closeTo(132.28, 0.01)
      // metric BMI for 165cm / 60kg is 22.04
      expect(features.BMI).to.be.closeTo(22.04, 0.02)
    })

    it('omits body features when the profile is missing', () => {
      const features = buildCycleFeatures({ cycleLengths: [28] })
      expect(features).to.not.have.property('BMI')
      expect(features).to.not.have.property('Age')
    })
  })

  describe('applyFeatureDefaults', () => {
    it('fills gaps from the trained medians', () => {
      const result = applyFeatureDefaults({ a: 1 }, ['a', 'b'], { b: 7 })
      expect(result).to.eql({ a: 1, b: 7 })
    })

    it('marks features as missing when there is no default', () => {
      const result = applyFeatureDefaults({}, ['a'], {})
      expect(Number.isNaN(result.a)).to.be.true()
    })

    it('drops features that are not in the model', () => {
      const result = applyFeatureDefaults({ a: 1, extra: 5 }, ['a'], {})
      expect(result).to.eql({ a: 1 })
    })
  })
})

describe('prediction entry points', () => {
  const withMenses = (cycleLengths) => ({
    cycleLengths,
    mensesPerCycle: cycleLengths.map(() => [
      bleedingDay('2021-04-01', 2),
      bleedingDay('2021-04-02', 1),
    ]),
  })

  describe('runCycleLengthModel', () => {
    it('returns null below the minimum number of cycles', () => {
      const cycleLengths = new Array(MIN_CYCLES_FOR_MODEL - 1).fill(28)
      expect(runCycleLengthModel({ cycleLengths })).to.be.null()
      expect(predictOvulationDay({ cycleLengths })).to.be.null()
    })

    it('returns null for no input at all', () => {
      expect(runCycleLengthModel()).to.be.null()
      expect(predictOvulationDay()).to.be.null()
    })

    it('predicts a whole number of days within the model clamps', () => {
      const days = runCycleLengthModel(withMenses([29, 28, 30, 27]))
      expect(days).to.be.a('number')
      expect(days % 1).to.eql(0)
      expect(days).to.be.within(18, 60)
    })

    it('predicts an ovulation day within the model clamps', () => {
      const day = predictOvulationDay(withMenses([29, 28, 30, 27]))
      expect(day).to.be.within(8, 40)
    })

    it('uses the luteal heuristic when cycles are regular', () => {
      const estimate = getOvulationEstimate(withMenses([28, 28, 28, 28]))
      expect(estimate).to.eql({ day: 14, source: 'luteal' })
    })

    it('uses the model when cycles are irregular', () => {
      const estimate = getOvulationEstimate(
        withMenses([26, 34, 29, 40, 25, 31])
      )
      expect(estimate.source).to.eql('model')
      expect(estimate.day).to.be.within(8, 40)
    })

    it('reacts to the cycle history it is given', () => {
      const short = runCycleLengthModel(withMenses([22, 23, 22, 23]))
      const long = runCycleLengthModel(withMenses([38, 39, 38, 39]))
      expect(short).to.be.below(long)
    })
  })

  describe('predictNextCycleLength', () => {
    it('defers to the personal average for regular cycles', () => {
      expect(predictNextCycleLength(withMenses([28, 28, 28, 28]))).to.be.null()
      expect(predictNextCycleLength(withMenses([30, 30, 29, 30, 31]))).to.be.null()
    })

    it('defers to the personal average for regular but unusual cycles', () => {
      // the raw model pulls a consistent 35 day cycle back towards the
      // population mean, which would be worse than the user's own average
      const history = withMenses([35, 34, 36, 35, 35, 34])
      expect(runCycleLengthModel(history)).to.be.below(34)
      expect(predictNextCycleLength(history)).to.be.null()
    })

    it('uses the model when cycles vary more than the model error', () => {
      const days = predictNextCycleLength(withMenses([26, 34, 29, 40, 25, 31]))
      expect(days).to.be.a('number')
    })

    it('never predicts outside the recorded cycle lengths', () => {
      const cycleLengths = [40, 58, 42, 60, 41, 55]
      const history = withMenses(cycleLengths)
      // unguarded, the model regresses this history towards a typical month
      expect(runCycleLengthModel(history)).to.be.below(40)
      expect(predictNextCycleLength(history)).to.be.within(40, 60)
    })

    it('returns null below the minimum number of cycles', () => {
      const cycleLengths = new Array(MIN_CYCLES_FOR_MODEL - 1).fill(28)
      expect(predictNextCycleLength({ cycleLengths })).to.be.null()
    })

    it('returns null for no input at all', () => {
      expect(predictNextCycleLength()).to.be.null()
    })
  })
})
