import type { Validator } from 'idonttrustlikethat';

import * as nsBye from '../Bye'
import * as nsHello from '../Hello'
import * as nsHi from '../Hi'
import * as nsGoodBye from '../GoodBye'
import * as nsGreeting from '../Greeting'
import * as nsWhatever from '../Whatever'

const hello: nsHello.Hello = 'Hello'
const greeting1a: nsGreeting.Greeting = 'Hello'
const greeting1b: nsGreeting.Greeting = hello

const goodBye: nsGoodBye.GoodBye = 'GoodBye'
const greeting2a: nsGreeting.Greeting = 'GoodBye'
const greeting2b: nsGreeting.Greeting = goodBye

const hi: nsHi.Hi = 'Hi'
const greeting3a: nsGreeting.Greeting = 'Hi'
const greeting3b: nsGreeting.Greeting = hi

const bye: nsBye.Bye = 'Bye'
const greeting4a: nsGreeting.Greeting = 'Bye'
const greeting4b: nsGreeting.Greeting = bye

const whatever1: nsWhatever.Whatever = { word: 'Yo' }
const greeting5: nsGreeting.Greeting = { _type: 'Whatever', word: 'Yo' }

function testGreeting<T extends nsGreeting.Greeting>(
  repr: string,
  a: nsGreeting.Greeting,
  b: nsGreeting.Greeting,
  expected: T,
  validator: Validator<T>,
  guard: (_v: any) => _v is T,
) {
  expect(a).toEqual(b)
  
  const result1 = validator.validate(repr)
  const result2 = nsGreeting.idtltGreeting.validate(repr)

  expect(result1.ok).toBe(true)
  expect(result2.ok).toBe(true)

  expect(guard(repr)).toBe(true)
  expect(nsGreeting.isGreeting(repr)).toBe(true)

  if (!result1.ok) {
    console.log(result1.errors)
  } else if (!result2.ok) {
    console.log(result2.errors)
  } else {
    const v1: T = result1.value
    const v2: nsGreeting.Greeting = result2.value

    expect(v1).toEqual(v2)
    expect(v1).toEqual(expected)
    expect(v1).toEqual(a)

    expect(guard(v1)).toBe(true)
    expect(guard(v2)).toBe(true)

    expect(nsGreeting.isGreeting(v1)).toBe(true)
    expect(nsGreeting.isGreeting(v2)).toBe(true)
  }
}

describe('Greeting', () => {
  it('should be validated for Hello', () => {
    testGreeting<nsHello.Hello>(
      'Hello',
      greeting1a,
      greeting1b,
      hello,
      nsHello.idtltHello,
      nsHello.isHello,
    )
  })

  it('should be validated for GoodBye', () => {
    testGreeting<nsGoodBye.GoodBye>(
      'GoodBye',
      greeting2a,
      greeting2b,
      goodBye,
      nsGoodBye.idtltGoodBye,
      nsGoodBye.isGoodBye,
    )
  })

  it('should be validated for Hi', () => {
    testGreeting<nsHi.Hi>(
      'Hi',
      greeting3a,
      greeting3b,
      hi,
      nsHi.idtltHi,
      nsHi.isHi,
    )
  })

  it('should be validated for Bye', () => {
    testGreeting<nsBye.Bye>(
      'Bye',
      greeting4a,
      greeting4b,
      bye,
      nsBye.idtltBye,
      nsBye.isBye,
    )
  })
})

describe('Whatever Yo', () => {
  it('should be validated', () => {
    const input = { word: 'Yo' }

    expect(nsWhatever.isWhatever(input)).toBe(true)
    expect(nsGreeting.isGreeting(input)).toBe(true)
    
    const result = nsWhatever.idtltWhatever.validate(input)

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(whatever1)
    }
  })

  it('should be validated as Greeting', () => {
    const result = nsGreeting.idtltGreeting.validate({
      _type: 'Whatever',
      word: 'Yo'
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(greeting5)
    }
  })
})
