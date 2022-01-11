import * as idtlt from 'idonttrustlikethat'

import * as nsBar from '../Bar'
import * as nsFoo from '../Foo'
import * as nsName from '../Name'

import * as testTransport from './Transport'

const date1 = '2021-01-25T21:06:45.459Z'
const date2 = '2021-01-26T21:06:45.459Z'

// Bar
const bar1: nsBar.Bar = {
  name: nsName.Name('One'),
  aliases: [],
  age: 2,
  amount: 456,
  transports: [],
  updated: new Date(date2),
  created: new Date(date1)
}

const bar2: nsBar.Bar = {
  name: nsName.Name('Two'),
  aliases: [nsName.Name('2'), nsName.Name('deux')],
  age: 3,
  transports: [],
  updated: new Date(date2),
  created: new Date(date1)
}

const bar3: nsBar.Bar = {
  name: nsName.Name('Three'),
  aliases: [],
  age: 4,
  amount: 6789,
  transports: [
    testTransport.transport1,
    testTransport.transport2,
    testTransport.transport3,
  ],
  updated: new Date(date2),
  created: new Date(date1)
}

describe('Bar', () => {
  it('should be validated for bar1', () => {
    const input = {
      name: 'One',
      aliases: [],
      age: 2,
      amount: 456,
      transports: [],
      updated: date2,
      created: date1
    }

    expect(nsBar.isBar(input)).toBe(false)
    // due to JSON representation of date != Date

    const result = nsBar.idtltBar.validate(input)

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(nsBar.isBar(result.value)).toBe(true)
      expect(result.value).toEqual(bar1)
    }

    // ---

    const raw = {
      name: 'One',
      aliases: [],
      age: 2,
      amount: 456,
      transports: [],
      updated: new Date(date2),
      created: new Date(date1)
    }

    expect(nsBar.isBar(raw)).toBe(true)

    if (nsBar.isBar(raw)) {
      const b: nsBar.Bar = { ...raw, name: nsName.Name(raw.name) }
      
      expect(b).toEqual(bar1)
    }
  })

  it('should be validated for bar2', () => {
    const input = {
      name: 'Two',
      aliases: ['2', 'deux'],
      age: 3,
      transports: [],
      updated: date2,
      created: date1
    }

    expect(nsBar.isBar(input)).toBe(false)
    // due to JSON representation of date != Date

    const result = nsBar.idtltBar.validate(input)

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(bar2)
    }

    // ---

    const raw = {
      name: 'Two',
      aliases: ['2', 'deux'],
      age: 3,
      transports: [],
      updated: new Date(date2),
      created: new Date(date1)
    }

    expect(nsBar.isBar(raw)).toBe(true)

    if (nsBar.isBar(raw)) {
      const b: nsBar.Bar = { ...raw, name: nsName.Name(raw.name) }

      expect(b).toEqual(bar2)
    }
  })

  it('should be validated for bar3', () => {
    const input = {
      name: 'Three',
      aliases: [],
      age: 4,
      amount: 6789,
      transports: [
        testTransport.transport1, 
        testTransport.transport2, 
        testTransport.transport3
      ],
      updated: date2,
      created: date1
    }

    expect(nsBar.isBar(input)).toBe(false)
    // due to JSON representation of date != Date

    const result = nsBar.idtltBar.validate(input)

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(bar3)
    }

    // ---

    const raw = {
      name: 'Three',
      aliases: [],
      age: 4,
      amount: 6789,
      transports: [
        testTransport.transport1,
        testTransport.transport2,
        testTransport.transport3,
      ],
      updated: new Date(date2),
      created: new Date(date1)
    }

    expect(nsBar.isBar(raw)).toBe(true)

    const b: nsBar.Bar = { ...raw, name: nsName.Name(raw.name) }

    expect(b).toEqual(bar3)
  })
})

// Foo
const foo1: nsFoo.Foo = {
  id: 1,
  namesp: [2, 'tuple'],
  row: ['tuple3', testTransport.transport1, new Date(date1)],
  score: 4,
  rates: {
    'key1': 1.23,
    'key2': 45
  }
}

const foo2: nsFoo.Foo = {
  id: 2,
  namesp: [3, 'value'],
  row: ['tuple3', testTransport.transport2, new Date(date2)],
  score: 'the best',
  rates: {
    'entry': 6
  }
}

describe('Foo', () => {
  it('should be validated for foo1', () => {
    const result = nsFoo.idtltFoo.validate({
      id: 1,
      namesp: [2, 'tuple'],
      row: ['tuple3', testTransport.transport1, date1],
      score: 4,
      rates: {
        'key1': 1.23,
        'key2': 45
      }
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(foo1)
    }

    // ---

    const raw = {
      id: 1,
      namesp: [2, 'tuple'],
      row: ['tuple3', testTransport.transport1, new Date(date1)],
      score: 4,
      rates: {
        'key1': 1.23,
        'key2': 45
      }
    }

    expect(nsFoo.isFoo(raw)).toBe(true)

    if (nsFoo.isFoo(raw)) {
      const f: nsFoo.Foo = raw
      
      expect(f).toEqual(foo1)
    }
  })

  it('should be validated for foo2', () => {
    const result = nsFoo.idtltFoo.validate({
      id: 2,
      namesp: [3, 'value'],
      row: ['tuple3', testTransport.transport2, date2],
      score: 'the best',
      rates: {
        'entry': 6
      }
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(foo2)
    }

    // ---

    const raw = {
      id: 2,
      namesp: [3, 'value'],
      row: ['tuple3', testTransport.transport2, new Date(date2)],
      score: 'the best',
      rates: {
        'entry': 6
      }
    }

    expect(nsFoo.isFoo(raw)).toBe(true)

    if (nsFoo.isFoo(raw)) {
      const f: nsFoo.Foo = raw
      
      expect(f).toEqual(foo2)
    }
  })
})
