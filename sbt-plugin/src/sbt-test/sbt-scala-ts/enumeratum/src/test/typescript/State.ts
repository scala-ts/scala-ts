import type { State } from '../State';
import * as alabama from '../Alabama';
import * as alaska from '../Alaska';

describe('State', () => {
  it('should be asserted as Alabama', () => {
    const value1: alabama.Alabama = 'AL'
    const value2: alabama.Alabama = alabama.AlabamaInhabitant

    expect(value1).toBe(value2)

    const state1: State = 'AL'
    const state2: State = alabama.AlabamaInhabitant

    expect(state1).toBe(state2)

    expect('AL' as alabama.Alabama).toEqual(value1)
    expect('AL' as alabama.Alabama).toEqual(state1)
  })

  it('should be asserted as Alaska', () => {
    const value1: alaska.Alaska = 'AK'
    const value2: alaska.Alaska = alaska.AlaskaInhabitant

    expect(value1).toBe(value2)

    const state1: State = 'AK'
    const state2: State = alaska.AlaskaInhabitant

    expect(state1).toBe(state2)

    expect('AK' as alaska.Alaska).toEqual(value1)
    expect('AK' as alaska.Alaska).toEqual(state1)
  })
})
