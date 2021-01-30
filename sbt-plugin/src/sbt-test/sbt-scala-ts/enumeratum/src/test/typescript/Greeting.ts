import type { Greeting } from '../Greeting';
import * as hello from '../Hello';
import * as hi from '../Hi';
import * as bye from '../Bye';
import * as goodbye from '../GoodBye';
import type { Whatever } from '../Whatever';

describe('Greeting', () => {
  it('should be asserted as Hello', () => {
    const value1: hello.Hello = 'Hello'
    const value2: hello.Hello = hello.HelloInhabitant

    expect(value1).toBe(value2)

    const greeting1: Greeting = 'Hello'
    const greeting2: Greeting = hello.HelloInhabitant

    expect(greeting1).toBe(greeting2)

    expect('Hello' as hello.Hello).toEqual(value1)
    expect('Hello' as hello.Hello).toEqual(greeting1)
  })

  it('should be asserted as Hi', () => {
    const value1: hi.Hi = 'Hi'
    const value2: hi.Hi = hi.HiInhabitant

    expect(value1).toBe(value2)

    const greeting1: Greeting = 'Hi'
    const greeting2: Greeting = hi.HiInhabitant

    expect(greeting1).toBe(greeting2)

    expect('Hi' as hi.Hi).toEqual(value1)
    expect('Hi' as hi.Hi).toEqual(greeting1)
  })

  it('should be asserted as Bye', () => {
    const value1: bye.Bye = 'Bye'
    const value2: bye.Bye = bye.ByeInhabitant

    expect(value1).toBe(value2)

    const greeting1: Greeting = 'Bye'
    const greeting2: Greeting = bye.ByeInhabitant

    expect(greeting1).toBe(greeting2)

    expect('Bye' as bye.Bye).toEqual(value1)
    expect('Bye' as bye.Bye).toEqual(greeting1)
  })

  it('should be asserted as GoodBye', () => {
    const value1: goodbye.GoodBye = 'GoodBye'
    const value2: goodbye.GoodBye = goodbye.GoodByeInhabitant

    expect(value1).toBe(value2)

    const greeting1: Greeting = 'GoodBye'
    const greeting2: Greeting = goodbye.GoodByeInhabitant

    expect(greeting1).toBe(greeting2)

    expect('GoodBye' as goodbye.GoodBye).toEqual(value1)
    expect('GoodBye' as goodbye.GoodBye).toEqual(greeting1)
  })

  it('should be asserted as Whatever', () => {
    const value1: Whatever = { word: 'Yo' }
    const value2: Whatever = { word: 'Bonjour' }

    expect(value1).not.toEqual(value2)

    const greeting1: Greeting = value1
    const greeting2: Greeting = value2

    expect(greeting1).not.toEqual(greeting2)

    expect(greeting1).toEqual(value1)
    expect(greeting2).toEqual(value2)
  })
})
