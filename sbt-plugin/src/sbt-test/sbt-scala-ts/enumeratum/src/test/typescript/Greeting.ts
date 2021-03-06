import { Greeting, isGreeting } from '../Greeting';
import * as hello from '../Hello';
import * as hi from '../Hi';
import * as bye from '../Bye';
import * as goodbye from '../GoodBye';
import { Whatever, isWhatever } from '../Whatever';

describe('Greeting', () => {
  it('should be asserted as Hello', () => {
    expect(hello.isHello('Hello')).toBe(true)
    expect(isGreeting('Hello')).toBe(true)

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
    expect(hi.isHi('Hi')).toBe(true)
    expect(isGreeting('Hi')).toBe(true)

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
    expect(bye.isBye('Bye')).toBe(true)
    expect(isGreeting('Bye')).toBe(true)

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
    expect(goodbye.isGoodBye('GoodBye')).toBe(true)
    expect(isGreeting('GoodBye')).toBe(true)

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
    expect(isWhatever({ word: 'Yo' })).toBe(true)
    expect(isGreeting({ word: 'Yo' })).toBe(true)

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
