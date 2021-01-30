import { TSWeekDay } from '../scalatsWeekDay'

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
})
