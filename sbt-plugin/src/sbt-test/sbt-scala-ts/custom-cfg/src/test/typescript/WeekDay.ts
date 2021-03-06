import { TSWeekDay, isTSWeekDay } from '../scalatsWeekDay'

describe('WeekDay', () => {
  it('should be asserted from valid literal', () => {
    expect('Mon' as TSWeekDay).toEqual(TSWeekDay.Mon)
    expect('Tue' as TSWeekDay).toEqual(TSWeekDay.Tue)
    expect('Wed' as TSWeekDay).toEqual(TSWeekDay.Wed)
    expect('Thu' as TSWeekDay).toEqual(TSWeekDay.Thu)
    expect('Fri' as TSWeekDay).toEqual(TSWeekDay.Fri)
    expect('Sat' as TSWeekDay).toEqual(TSWeekDay.Sat)
    expect('Sun' as TSWeekDay).toEqual(TSWeekDay.Sun)
  })

  it('should be checked', () => {
    expect(isTSWeekDay('Foo')).toBe(false)

    expect(isTSWeekDay('Mon')).toBe(true)
    expect(isTSWeekDay('Tue')).toBe(true)
    expect(isTSWeekDay('Wed')).toBe(true)
    expect(isTSWeekDay('Thu')).toBe(true)
    expect(isTSWeekDay('Fri')).toBe(true)
    expect(isTSWeekDay('Sat')).toBe(true)
    expect(isTSWeekDay('Sun')).toBe(true)
  })
})
