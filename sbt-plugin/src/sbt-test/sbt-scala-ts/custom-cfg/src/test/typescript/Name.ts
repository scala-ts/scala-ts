import { TSName, isTSName } from '../scalatsName'

describe('Name', () => {
  it('should be created from string', () => {
    const n: TSName = TSName('foo')

    expect(n.__tag).toEqual(undefined/* no runtime value but compiled */)
    expect(n).toEqual('foo')
  })
})
