import { ConstantsInhabitant } from '../Constants';
import { Grade, isGrade } from '../Grade';

describe('Constants', () => {
  it('should declare simple values', () => {
    expect(ConstantsInhabitant.code).toEqual(1)

    expect(ConstantsInhabitant.name).toEqual('foo')

    // Value class
    expect(isGrade(ConstantsInhabitant.LowerGrade)).toBe(true)

    const lowerGrade: Grade = ConstantsInhabitant.LowerGrade

    expect(lowerGrade).toEqual(0)
  })

  it('should declare complex values', () => {
    // List
    expect(ConstantsInhabitant.list instanceof Array).toBe(true)
    expect(ConstantsInhabitant.list.
           includes(ConstantsInhabitant.LowerGrade)).toBe(true)
    expect(ConstantsInhabitant.list.length).toEqual(1)

    // Set
    expect(ConstantsInhabitant.set instanceof Set).toBe(true)
    expect(ConstantsInhabitant.set.has("lorem")).toBe(true)
    expect(ConstantsInhabitant.set.has("ipsum")).toBe(true)
    expect(ConstantsInhabitant.set.size).toEqual(2)

    // Dictionary
    const dict: Readonly<Partial<Record<string, string>>> = // Check inference
      ConstantsInhabitant.dict

    expect(dict['A']).toEqual("value #1")
    expect(dict['B']).toEqual(ConstantsInhabitant.name)

    expect((dict as any)['C']).toEqual(undefined)

    // More complex list
    expect(ConstantsInhabitant.listOfDict instanceof Array).toBe(true)
    expect(ConstantsInhabitant.listOfDict.length).toEqual(2)

    const first: Readonly<Partial<Record<string, string>>> | undefined =
      ConstantsInhabitant.listOfDict[0]

    expect(first).toEqual({ 'title': "Foo", 'description': "..." })

    const second: Readonly<Partial<Record<string, string>>> | undefined =
      ConstantsInhabitant.listOfDict[1]

    expect(second).toEqual({ 'title': "Bar", 'description': "..." })

    expect(ConstantsInhabitant.listOfDict[2]).toEqual(undefined)
  })
})
