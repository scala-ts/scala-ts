import * as nsWeekDay from '../WeekDay'

const mon: nsWeekDay.WeekDay = 'Mon'
const tue: nsWeekDay.WeekDay = 'Tue'
const wed: nsWeekDay.WeekDay = 'Wed'
const thu: nsWeekDay.WeekDay = 'Thu'
const fri: nsWeekDay.WeekDay = 'Fri'
const sat: nsWeekDay.WeekDay = 'Sat'
const sun: nsWeekDay.WeekDay = 'Sun'

describe('WeekDay', () => {
  it('should be validated for Monday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Mon')

    expect(nsWeekDay.isWeekDay('Mon')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(mon)
    }
  })

  it('should be validated for Tuesday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Tue')

    expect(nsWeekDay.isWeekDay('Tue')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(tue)
    }
  })

  it('should be validated for Wednesday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Wed')

    expect(nsWeekDay.isWeekDay('Wed')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(wed)
    }
  })

  it('should be validated for Thurday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Thu')

    expect(nsWeekDay.isWeekDay('Thu')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(thu)
    }
  })

  it('should be validated for Friday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Fri')

    expect(nsWeekDay.isWeekDay('Fri')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(fri)
    }
  })

  it('should be validated for Saturday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Sat')

    expect(nsWeekDay.isWeekDay('Sat')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(sat)
    }
  })

  it('should be validated for Sunday', () => {
    const result = nsWeekDay.idtltWeekDay.validate('Sun')

    expect(nsWeekDay.isWeekDay('Sun')).toBe(true)
    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(sun)
    }
  })
})
