import { CaseClassBar, isCaseClassBar } from '../CaseClassBar';

const bar1: CaseClassBar = {
  firstName: 'First1',
  lastName: 'Last1',
  grade: 1,
  time: '12:34'
}

describe('Bar', () => {
  it('should be asserted for bar1', () => {
    const result = {
      firstName: 'First1',
      lastName: 'Last1',
      grade: 1,
      time: '12:34'
    }

    expect(!!isCaseClassBar({})).toBe(false)
    expect(isCaseClassBar(result)).toBe(true)

    const barRes: CaseClassBar = result

    expect(barRes).toEqual(bar1)
  })
})
