import chai from 'chai'
import dirtyChai from 'dirty-chai'

import { compileBooster, predict, MODEL_FORMAT } from '../lib/prediction/xgboost'
import { loadModel, clearModelCache } from '../lib/prediction/models'
import reference from './fixtures/xgboost-reference.json'

const expect = chai.expect
chai.use(dirtyChai)

const boosters = {
  cycleLength: require('../assets/models/cycle_length.json'),
  ovulationDay: require('../assets/models/ovulation_day.json'),
}

describe('xgboost runtime', () => {
  describe('matches the Python reference implementation', () => {
    Object.keys(reference).forEach((modelName) => {
      it(`for the ${modelName} model`, () => {
        const booster = compileBooster(boosters[modelName])
        reference[modelName].cases.forEach((testCase) => {
          const result = predict(booster, testCase.features)
          expect(result).to.be.closeTo(
            testCase.expected,
            1e-6,
            `${modelName} / ${testCase.label}`
          )
        })
      })
    })
  })

  describe('compileBooster', () => {
    it('rejects an unknown format', () => {
      expect(() => compileBooster({ format: 'nope', trees: [{ leaf: 1 }] }))
        .to.throw(/Unsupported model format/)
    })

    it('rejects a booster without trees', () => {
      expect(() => compileBooster({ format: MODEL_FORMAT, trees: [] }))
        .to.throw(/no trees/)
    })

    it('rejects a node whose children do not resolve', () => {
      const payload = {
        format: MODEL_FORMAT,
        base_score: 0,
        feature_names: ['a'],
        trees: [
          {
            nodeid: 0,
            split: 'a',
            split_condition: 1,
            yes: 1,
            no: 2,
            missing: 9,
            children: [
              { nodeid: 1, leaf: -1 },
              { nodeid: 2, leaf: 1 },
            ],
          },
        ],
      }
      expect(() => compileBooster(payload)).to.throw(/unresolved child id/)
    })
  })

  describe('predict', () => {
    const stump = compileBooster({
      format: MODEL_FORMAT,
      base_score: 10,
      feature_names: ['a'],
      trees: [
        {
          nodeid: 0,
          split: 'a',
          split_condition: 5,
          yes: 1,
          no: 2,
          missing: 3,
          children: [
            { nodeid: 1, leaf: -1 },
            { nodeid: 2, leaf: 1 },
            { nodeid: 3, leaf: 100 },
          ],
        },
      ],
    })

    it('takes the yes branch below the split condition', () => {
      expect(predict(stump, { a: 4.999 })).to.eql(9)
    })

    it('takes the no branch at the split condition', () => {
      expect(predict(stump, { a: 5 })).to.eql(11)
    })

    it('takes the missing branch for undefined, null and NaN', () => {
      expect(predict(stump, {})).to.eql(110)
      expect(predict(stump, { a: null })).to.eql(110)
      expect(predict(stump, { a: NaN })).to.eql(110)
    })

    it('does not treat zero as missing', () => {
      expect(predict(stump, { a: 0 })).to.eql(9)
    })
  })

  describe('loadModel', () => {
    beforeEach(clearModelCache)
    after(clearModelCache)

    it('exposes metadata alongside the compiled booster', () => {
      const model = loadModel('cycleLength')
      expect(model.target).to.eql('Next_LengthofCycle')
      expect(model.clamps).to.eql({ min: 18, max: 60 })
      expect(model.featureNames).to.have.length(21)
      expect(model.metrics.rmse).to.be.a('number')
      expect(model.metrics.split).to.eql('5-fold grouped by person')
    })

    it('returns the same compiled instance on repeated calls', () => {
      expect(loadModel('ovulationDay')).to.equal(loadModel('ovulationDay'))
    })

    it('throws for an unknown model', () => {
      expect(() => loadModel('nope')).to.throw(/Unknown prediction model/)
    })
  })
})
