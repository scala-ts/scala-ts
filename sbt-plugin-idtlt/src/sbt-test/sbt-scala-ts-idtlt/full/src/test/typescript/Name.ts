import * as idtlt from 'idonttrustlikethat'

import * as nsName from '../Name'

describe('Name', () => {
  it('should be created from string', () => {
    const n: nsName.Name = nsName.Name('Foo')

    expect(n).toEqual('Foo')

    const result = nsName.idtltName.validate('Foo')

    if (!result.ok) {
      console.log(result.errors)
    } else {
      const parsed: nsName.Name = result.value
      
      expect(parsed).toEqual(n)
    }
  })
})
