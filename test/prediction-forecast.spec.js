import chai from 'chai'
import dirtyChai from 'dirty-chai'

import {
  buildFertileWindow,
  buildTimedForecast,
  enumerateDateRange,
  isProfileComplete,
  predictedPeriodStart,
} from '../lib/prediction/forecast'

const expect = chai.expect
chai.use(dirtyChai)

describe('prediction forecast helpers', () => {
  describe('buildFertileWindow', () => {
    it('spans five days before ovulation and one day after', () => {
      expect(buildFertileWindow(14, 28)).to.eql({
        start: 9,
        end: 15,
        ovulationDay: 14,
      })
    })

    it('does not start before day 1', () => {
      expect(buildFertileWindow(3, 28).start).to.eql(1)
    })

    it('does not run past the predicted cycle length', () => {
      expect(buildFertileWindow(28, 28).end).to.eql(28)
    })
  })

  describe('predictedPeriodStart', () => {
    it('returns the middle date of the first predicted range', () => {
      expect(
        predictedPeriodStart([
          ['2021-06-28', '2021-06-29', '2021-06-30'],
        ])
      ).to.eql('2021-06-29')
    })

    it('returns null without predictions', () => {
      expect(predictedPeriodStart([])).to.be.null()
    })
  })

  describe('buildTimedForecast', () => {
    it('dates the fertile window from the current cycle start', () => {
      const result = buildTimedForecast({
        todayDateString: '2021-06-10',
        cycleStartDate: '2021-06-01',
        predictedLength: 28,
        ovulationDay: 14,
        predictedMenses: [['2021-06-29']],
      })
      expect(result).to.eql({
        ovulationDate: '2021-06-14',
        fertileStartDate: '2021-06-09',
        fertileEndDate: '2021-06-15',
        windowIsNextCycle: false,
      })
    })

    it('projects onto the next cycle when the window has already ended', () => {
      const result = buildTimedForecast({
        todayDateString: '2021-06-20',
        cycleStartDate: '2021-06-01',
        predictedLength: 28,
        ovulationDay: 14,
        predictedMenses: [['2021-06-29']],
      })
      expect(result.windowIsNextCycle).to.be.true()
      expect(result.ovulationDate).to.eql('2021-07-12')
      expect(result.fertileStartDate).to.eql('2021-07-07')
      expect(result.fertileEndDate).to.eql('2021-07-13')
    })
  })

  describe('enumerateDateRange', () => {
    it('includes both ends', () => {
      expect(enumerateDateRange('2021-06-09', '2021-06-11')).to.eql([
        '2021-06-09',
        '2021-06-10',
        '2021-06-11',
      ])
    })
  })

  describe('isProfileComplete', () => {
    it('requires age, height and weight', () => {
      expect(isProfileComplete({ age: 30, heightCm: 165, weightKg: 60 })).to.be
        .true()
      expect(isProfileComplete({ age: 30, heightCm: 165 })).to.be.false()
      expect(isProfileComplete(null)).to.be.false()
    })
  })
})
