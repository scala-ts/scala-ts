import { ConstantsInhabitant } from '../Constants'
import * as nsName from '../Name'

describe('Constants', () => {
  it('should declare simple values', () => {
    expect(ConstantsInhabitant.code).toEqual(1)

    // Value class
    const unknownName: nsName.Name = ConstantsInhabitant.UnknownName

    expect(nsName.isName(unknownName)).toBe(true)
    expect(unknownName).toEqual('unknown')

    const defaultName: nsName.Name = ConstantsInhabitant.defaultName

    expect(nsName.isName(defaultName)).toBe(true)
    expect(defaultName).toEqual('default')
  })

  it('should declare more complex values', () => {
    // List
    expect(ConstantsInhabitant.list instanceof Array).toBe(true)
    expect(ConstantsInhabitant.list.length).toEqual(2)

    expect(ConstantsInhabitant.list.
           includes(ConstantsInhabitant.code)).toBe(true)
    expect(ConstantsInhabitant.list.includes(2)).toBe(true)

    // Dictionary
    const dict: Readonly<Map<string, ReadonlyArray<nsName.Name>>> = ConstantsInhabitant.dict

    const specific: ReadonlyArray<nsName.Name> | undefined = dict.get('specific')

    if (!specific) {
      fail('Missing specific')
    }
    
    expect(specific.includes(ConstantsInhabitant.UnknownName)).toBe(true)
    expect(specific.includes(ConstantsInhabitant.defaultName)).toBe(true)
    expect(specific.includes(nsName.Name("*"))).toBe(true)
    expect(specific.length).toEqual(3)

    const invalid: ReadonlyArray<nsName.Name> | undefined = dict.get('invalid')
    expect(invalid).toEqual([ nsName.Name("failed") ])
  })

  it('should declare merged list & set', () => {

    expect(ConstantsInhabitant.excluded.length).toEqual(2)
    expect(ConstantsInhabitant.excluded.includes("foo")).toBe(true)
    expect(ConstantsInhabitant.excluded.includes("bar")).toBe(true)

    expect(ConstantsInhabitant.filtered.length).toEqual(3)
    expect(ConstantsInhabitant.filtered.includes("foo")).toBe(true)
    expect(ConstantsInhabitant.filtered.includes("bar")).toBe(true)
    expect(ConstantsInhabitant.filtered.includes("filtered")).toBe(true)

    const names: ReadonlyArray<nsName.Name> = ConstantsInhabitant.names

    expect(names.length).toEqual(3)
    expect(names.includes(ConstantsInhabitant.UnknownName)).toBe(true)
    expect(names.includes(ConstantsInhabitant.defaultName)).toBe(true)
    expect(names.includes(nsName.Name("test"))).toBe(true)
  })
})
