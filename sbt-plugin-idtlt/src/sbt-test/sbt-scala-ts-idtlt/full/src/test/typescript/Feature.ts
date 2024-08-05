import * as nsFeature from '../Feature'
import * as nsBarNum from '../BarNum'
import * as nsFooLure from '../FooLure'

describe('Feature', () => {
  it('should expose the known literal values', () => {
    const knownValues: ReadonlySet<nsFeature.Feature> = nsFeature.idtltFeatureUnionKnownValues

    expect(knownValues).toEqual(new Set([ 'BarNum', 'FooLure' ]))
  })

  it('should be BarNum', () => {
    const input = 'BarNum'

    expect(nsBarNum.isBarNum(input)).toBe(true)
    expect(nsFeature.isFeature(input)).toBe(true)
    expect(nsFooLure.isFooLure(input)).toBe(false)

    if (nsBarNum.isBarNum(input) && nsFeature.isFeature(input)) {
      const f: nsFeature.Feature = input
      const b: nsBarNum.BarNum = input

      expect(f).toEqual(b)
      expect(b).toEqual(nsBarNum.BarNumInhabitant)
    }
  })

  it('should be FooLure', () => {
    const input = 'FooLure'

    expect(nsFooLure.isFooLure(input)).toBe(true)
    expect(nsBarNum.isBarNum(input)).toBe(false)
    expect(nsFeature.isFeature(input)).toBe(true)

    if (nsFooLure.isFooLure(input) && nsFeature.isFeature(input)) {
      const f: nsFeature.Feature = input
      const l: nsFooLure.FooLure = input

      expect(f).toEqual(l)
      expect(l).toEqual(nsFooLure.FooLureInhabitant)
    }
  })
})
