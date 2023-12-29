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

    // Much more complex map
    const seqOfMap: ReadonlyArray<Readonly<Partial<Record<TSName, string>>>> =
      TSConstantsInhabitant._seqOfMap

    expect(seqOfMap.length).toEqual(2)

    const map1: Readonly<Partial<Record<TSName, string>>> | undefined = seqOfMap[0]

    if (!map1) {
      fail('Missing map1')
    }

    const lorem: TSName = TSName("lorem")
    const dolor = TSName("dolor")

    expect(map1[lorem]).toEqual("lorem")
    expect(map1[defaultName]).toEqual("ipsum")
    expect(map1[dolor]).toEqual(undefined)

    const map2: Readonly<Partial<Record<TSName, string>>> | undefined = seqOfMap[1]

    if (!map2) {
      fail('Missing map2')
    }

    expect(map2[dolor]).toEqual("value")
    expect(map2[defaultName]).toEqual(undefined)
  })
})
