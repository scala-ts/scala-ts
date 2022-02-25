import { TSName, isTSName } from '../scalatsName'
import { TSConstantsInhabitant } from '../scalatsConstants'

describe('Constants', () => {
  it('should be declared as tagged type', () => {
    const defaultName: TSName = TSConstantsInhabitant._DefaultName

    expect(isTSName(defaultName)).toBe(true)
    expect(defaultName).toEqual("default")

    const list: ReadonlyArray<TSName> = TSConstantsInhabitant._list

    expect(list.length).toEqual(2)
    expect(list.includes(defaultName)).toBe(true)
    expect(list.includes(TSName("test"))).toBe(true)
  })
})
