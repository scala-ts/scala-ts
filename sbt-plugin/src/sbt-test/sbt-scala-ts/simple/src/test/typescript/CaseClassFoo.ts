import type { CaseClassFoo } from '../CaseClassFoo';
import type { CaseClassBar } from '../CaseClassBar';

const time1: number = Date.now()
const localDate1: Date = new Date()
const instant1: Date = new Date()
const localDateTime1: Date = new Date()
const offsetDateTime1: Date = new Date()
const zonedDateTime1: Date = new Date()
const ts1: Date = new Date()

const foo1: CaseClassFoo = {
  id: 'Foo#1',
  name: 'One',
  i: 1,
  flag: 100,
  score: 1.23,
  time: time1,
  localDate: localDate1,
  instant: instant1,
  localDateTime: localDateTime1,
  offsetDateTime: offsetDateTime1,
  zonedDateTime: zonedDateTime1,
  ts: ts1,
  tuple2: [ "T2", 20 ],
  tuple3: [ localDate1, offsetDateTime1, {} as CaseClassFoo ],
}

const time2: number = Date.now()
const localDate2: Date = new Date()
const instant2: Date = new Date()
const localDateTime2: Date = new Date()
const offsetDateTime2: Date = new Date()
const zonedDateTime2: Date = new Date()
const ts2: Date = new Date()

const bar1: CaseClassBar = {
  firstName: 'First2',
  lastName: 'Last2',
}

const foo2: CaseClassFoo = {
  id: 'Foo#2',
  name: 'Two',
  i: 2,
  flag: 200,
  score: 34.5,
  time: time2,
  localDate: localDate2,
  instant: instant2,
  localDateTime: localDateTime2,
  offsetDateTime: offsetDateTime2,
  zonedDateTime: zonedDateTime2,
  ts: ts2,
  tuple2: [ "T2_2", 30 ],
  tuple3: [ localDate1, offsetDateTime1, foo1 ],
  bar: {
    firstName: 'First2',
    lastName: 'Last2',
  }
}

describe('Foo', () => {
  it('should be asserted for foo1', () => {
    const result = {
      id: 'Foo#1',
      name: 'One',
      i: 1,
      flag: 100,
      score: 1.23,
      time: time1,
      localDate: localDate1,
      instant: instant1,
      localDateTime: localDateTime1,
      offsetDateTime: offsetDateTime1,
      zonedDateTime: zonedDateTime1,
      ts: ts1,
      tuple2: [ "T2", 20 ],
      tuple3: [ localDate1, offsetDateTime1, {} ],
    }

    expect(result as CaseClassFoo).toEqual(foo1)
  })

  it('should be asserted for foo2', () => {
    const result = {
      id: 'Foo#2',
      name: 'Two',
      i: 2,
      flag: 200,
      score: 34.5,
      time: time2,
      localDate: localDate2,
      instant: instant2,
      localDateTime: localDateTime2,
      offsetDateTime: offsetDateTime2,
      zonedDateTime: zonedDateTime2,
      ts: ts2,
      tuple2: [ "T2_2", 30 ],
      tuple3: [ localDate1, offsetDateTime1, foo1 ],
      bar: {
        firstName: 'First2',
        lastName: 'Last2',
      }
    }

    expect(result as CaseClassFoo).toEqual(foo2)
  })
})
