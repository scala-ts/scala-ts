import * as idtlt from 'idonttrustlikethat'

import * as nsAlabama from '../Alabama'
import * as nsAlaska from '../Alaska'
import * as nsState from '../State'

const alabama: nsAlabama.Alabama = 'AL'
const state1a: nsState.State = 'AL'
const state1b: nsState.State = alabama

const alaska: nsAlaska.Alaska = 'AK'
const state2a: nsState.State = 'AK'
const state2b: nsState.State = alaska

function testState<T extends nsState.State>(
  repr: string,
  a: nsState.State,
  b: nsState.State,
  expected: T,
  validator: idtlt.Validator<T>,
) {
  expect(a).toEqual(b)
  
  const result1 = validator.validate(repr)
  const result2 = nsState.idtltState.validate(repr)

  expect(result1.ok).toBe(true)
  expect(result2.ok).toBe(true)

  if (!result1.ok) {
    console.log(result1.errors)
  } else if (!result2.ok) {
    console.log(result2.errors)
  } else {
    const v1: T = result1.value
    const v2: nsState.State = result2.value

    expect(v1).toEqual(v2)
    expect(v1).toEqual(expected)
    expect(v1).toEqual(a)
  }
}

describe('State', () => {
  it('should be validated for Alabama', () => {
    testState<nsAlabama.Alabama>(
      'AL',
      state1a,
      state1b,
      alabama,
      nsAlabama.idtltAlabama,
    )
  })

  it('should be validated for Alaska', () => {
    testState<nsAlaska.Alaska>(
      'AK',
      state2a,
      state2b,
      alaska,
      nsAlaska.idtltAlaska,
    )
  })

  it('should expose the known literal values', () => {
    const knownValues: Array<nsState.State> = nsState.idtltStateKnownValues

    expect(knownValues).toEqual([ 'AL', 'AK' ])
  })
})
