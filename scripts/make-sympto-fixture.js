// Writes test/fixtures/sympto-reference.json: what drip's sympto returns for the
// example cycles shipped with sympto and for generated multi-cycle histories.
// The Kotlin port (android/.../data/fertility/SymptoReferenceTest.kt) must match
// every case.  Run: node scripts/make-sympto-fixture.js
const fs = require('fs')
const path = require('path')
const { LocalDate } = require('js-joda')

// The minus-8 rule counts cycle days with compareTo, which js-joda answers in
// months across a month boundary. The port counts real days, and so does this
// reference. The sign is unchanged, so every other comparison behaves the same.
LocalDate.prototype.compareTo = function (other) {
  return this.toEpochDay() - other.toEpochDay()
}

const sympto = require('sympto')
const mucusBook = require('sympto/test/mucus-temp-fixtures')
const cervixBook = require('sympto/test/cervix-temp-fixtures')

let seed = 20261009
function rand () {
  seed = (seed * 1103515245 + 12345) % 2147483648
  return seed / 2147483648
}
const pick = (low, high) => low + Math.floor(rand() * (high - low + 1))
const gauss = () => {
  let sum = 0
  for (let i = 0; i < 6; i++) sum += rand()
  return (sum - 3) / Math.sqrt(0.5)
}

function makeCycle (start, secondary, truncateAt, late) {
  const length = late ? pick(31, 36) : pick(23, 36)
  const ovulation = late ? pick(17, 20) : pick(Math.max(9, length - 17), length - 10)
  const anovulatory = !late && rand() < 0.12
  const periodLength = pick(3, 6)
  const base = 36.2 + rand() * 0.4
  const shift = 0.15 + rand() * 0.35
  const temperatureRate = late ? 0.97 : 0.6 + rand() * 0.4
  const days = []
  for (let d = 1; d <= Math.min(truncateAt || length, length); d++) {
    if (d > 1 && rand() < 0.08) continue
    const day = { date: start.plusDays(d - 1).toString() }
    if (d <= periodLength) day.bleeding = d === 1 ? pick(1, 3) : pick(0, 3)
    if (rand() < temperatureRate) {
      let t = base + gauss() * 0.06 + (!anovulatory && d > ovulation ? shift : 0)
      if (rand() < 0.04) t += 0.4 + rand() * 0.3
      day.temperature = Math.round(t * 100) / 100
    }
    const rel = d - ovulation
    if (d > periodLength && rand() < 0.85) {
      if (secondary === 'mucus') {
        let m = rel < -6 ? pick(0, 1) : rel < -3 ? pick(1, 3) : rel <= 0 ? pick(3, 4) : rel <= 2 ? pick(1, 3) : pick(0, 1)
        if (anovulatory) m = pick(0, 2)
        if (rand() < 0.07) m = pick(0, 4)
        day.mucus = m
      } else {
        const fertile = !anovulatory && rel >= -4 && rel <= 1
        day.cervix = fertile
          ? { opening: pick(1, 2), firmness: rand() < 0.8 ? 1 : 0 }
          : { opening: rand() < 0.08 ? 1 : 0, firmness: rand() < 0.08 ? 1 : 0 }
      }
    }
    days.push(day)
  }
  return { days, length }
}

function generated (index) {
  const secondary = rand() < 0.6 ? 'mucus' : 'cervix'
  // every 16th history ovulates late for 13+ cycles, so the minus-8 rule can lengthen the pre phase
  const late = index % 16 === 8
  const count = late ? pick(16, 18) : index % 8 === 0 ? pick(13, 16) : pick(1, 4)
  let start = LocalDate.of(2024, 1, 1).plusDays(pick(0, 700))
  const cycles = []
  for (let c = 0; c < count; c++) {
    const truncateAt = c === count - 1 && rand() < 0.6 ? pick(2, 30) : null
    const { days, length } = makeCycle(start, secondary, truncateAt, late)
    cycles.push(days)
    start = start.plusDays(length)
  }
  return { name: `generated ${index}`, secondary, cycles }
}

// drip drops cervix values missing opening or firmness before calling sympto.
const clean = cycle => cycle.map(day => {
  const copy = { ...day }
  if (copy.cervix && (typeof copy.cervix.opening !== 'number' || typeof copy.cervix.firmness !== 'number')) {
    delete copy.cervix
  }
  return copy
})

function bookCases (book, secondary, previous) {
  return Object.entries(book).flatMap(([name, cycle]) => [
    { name: `${secondary} ${name}`, secondary, cycles: [clean(cycle)] },
    { name: `${secondary} ${name} after a shift`, secondary, cycles: [clean(previous), clean(cycle)] }
  ])
}

function phase (p) {
  if (!p) return null
  const days = p.cycleDays
  return {
    start: p.start.date,
    end: p.end ? p.end.date : null,
    days: days.length,
    first: days.length ? days[0].date : null,
    last: days.length ? days[days.length - 1].date : null
  }
}

function expected (status) {
  const t = status.temperatureShift
  const s = status.mucusShift || status.cervixShift
  return {
    pre: phase(status.phases.preOvulatory),
    peri: phase(status.phases.periOvulatory),
    post: phase(status.phases.postOvulatory),
    temperature: t
      ? { rule: t.rule, ltl: t.ltl, firstHigh: t.firstHighMeasurementDay.date, complete: t.evaluationCompleteDay.date }
      : null,
    secondary: s ? { peak: (s.mucusPeak || s.cervixPeakBeforeShift).date, complete: s.evaluationCompleteDay.date } : null
  }
}

const inputs = [
  ...bookCases(mucusBook, 'mucus', mucusBook.cycleWithFhm),
  ...bookCases(cervixBook, 'cervix', cervixBook.cervixShiftAndFhmOnSameDay),
  ...Array.from({ length: 160 }, (_, i) => generated(i))
]

const cases = inputs.map(input => {
  const cycles = input.cycles
  const status = sympto({
    cycle: cycles[cycles.length - 1],
    previousCycle: cycles.length > 1 ? cycles[cycles.length - 2] : undefined,
    earlierCycles: cycles.slice(0, -2),
    secondarySymptom: input.secondary
  })
  return { ...input, expected: expected(status) }
})

const count = test => cases.filter(c => test(c.expected)).length
console.log(`${cases.length} cases`)
console.log(`  pre phase: ${count(e => e.pre)}, pre only: ${count(e => e.pre && !e.peri)}, ` +
  `minus-8 shortened/lengthened: ${count(e => e.pre && e.pre.end < LocalDate.parse(e.pre.start).plusDays(4).toString())}/` +
  `${count(e => e.pre && e.pre.end > LocalDate.parse(e.pre.start).plusDays(4).toString())}`)
console.log(`  post phase: ${count(e => e.post)}, rules 0/1/2: ` +
  [0, 1, 2].map(r => count(e => e.temperature && e.temperature.rule === r)).join('/'))
console.log(`  cervix cases: ${cases.filter(c => c.secondary === 'cervix').length}`)

const out = path.join(__dirname, '..', 'test', 'fixtures', 'sympto-reference.json')
fs.writeFileSync(out, '[\n' + cases.map(c => JSON.stringify(c)).join(',\n') + '\n]\n')
console.log(`wrote ${path.relative(process.cwd(), out)} (${(fs.statSync(out).size / 1024).toFixed(0)} KB)`)
