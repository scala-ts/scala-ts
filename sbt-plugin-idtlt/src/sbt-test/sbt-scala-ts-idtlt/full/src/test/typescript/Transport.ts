import * as nsTransport from '../Transport'
import * as nsTrainLine from '../TrainLine'
import * as nsBusLine from '../BusLine'

// TrainLine
const trainLine1: nsTrainLine.TrainLine = {
  name: 'First',
  startStationId: 'start1',
  endStationId: 'end1'
}

describe('TrainLine', () => {
  const json = {
    name: 'First',
    startStationId: 'start1',
    endStationId: 'end1'
  }

  it('should be validated for trainLine1', () => {
    const result = nsTrainLine.idtltTrainLine.validate(json)

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(trainLine1)

      expect(nsTransport.isTransport(result.value)).toBe(true)
      expect(nsTrainLine.isTrainLine(result.value)).toBe(true)
      expect(nsBusLine.isBusLine(result.value)).toBe(false)
    }
  })

  it('should not be validated as discriminated for trainLine1', () => {
    const result = nsTrainLine.idtltDiscriminatedTrainLine.validate(json)

    expect(result.ok).toBe(false)

    if (!result.ok) {
      expect(result.errors).toStrictEqual([
        { path: '_type', message: 'Expected "TrainLine", got undefined' }
      ])
    }
  })

  it('should be discriminated from trainLine1', () => {
    const discriminated: nsTrainLine.DiscriminatedTrainLine =
      nsTrainLine.discriminatedTrainLine(trainLine1)

    expect(nsTrainLine.idtltTrainLine.validate(discriminated).ok).toBe(true)

    expect(nsTrainLine.idtltTrainLine.validate({
      ...json, _type: 'TrainLine'
    }).ok).toBe(true)
  })
})

// BusLine
const busLine1: nsBusLine.BusLine = {
  id: 1,
  name: 'One',
  stopIds: []
}

const busLine2: nsBusLine.BusLine = {
  id: 2,
  name: 'Two',
  stopIds: [ 'start1', 'end2' ]
}

describe('BusLine', () => {
  const json = {
    id: 1,
    name: 'One',
    stopIds: []
  }

  it('should be validated for busLine1', () => {
    const result = nsBusLine.idtltBusLine.validate(json)

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(busLine1)

      expect(nsTransport.isTransport(result.value)).toBe(true)
      expect(nsTrainLine.isTrainLine(result.value)).toBe(false)
      expect(nsBusLine.isBusLine(result.value)).toBe(true)
    }
  })

  it('should be validated for busLine2', () => {
    const result = nsBusLine.idtltBusLine.validate({
      id: 2,
      name: 'Two',
      stopIds: [ 'start1', 'end2' ]
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(busLine2)
    }
  })

  it('should not be validated as discriminated for busLine1', () => {
    const result = nsBusLine.idtltDiscriminatedBusLine.validate(json)

    expect(result.ok).toBe(false)

    if (!result.ok) {
      expect(result.errors).toStrictEqual([
        { path: '_type', message: 'Expected "BusLine", got undefined' }
      ])
    }
  })

  it('should be discriminated from busLine1', () => {
    const discriminated: nsBusLine.DiscriminatedBusLine =
      nsBusLine.discriminatedBusLine(busLine1)

    expect(nsBusLine.idtltBusLine.validate(discriminated).ok).toBe(true)

    expect(nsBusLine.idtltBusLine.validate({
      ...json, _type: 'BusLine'
    }).ok).toBe(true)
  })
})

// Transport
export const transport1: nsTransport.Transport = {
  _type: 'TrainLine',
  ...trainLine1
}

export const transport2: nsTransport.Transport = {
  _type: 'BusLine',
  ...busLine1
}

export const transport3: nsTransport.Transport = {
  _type: 'BusLine',
  ...busLine2
}

describe('Transport', () => {
  it('should be validated for transport1/trainLine1', () => {
    const result = nsTransport.idtltTransport.validate({
      _type: 'TrainLine',
      name: 'First',
      startStationId: 'start1',
      endStationId: 'end1'
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(transport1)

      expect(nsTransport.isTransport(result.value)).toBe(true)
      expect(nsTrainLine.isTrainLine(result.value)).toBe(true)
      expect(nsBusLine.isBusLine(result.value)).toBe(false)
    }
  })

  it('should be validated for transport2/busLine1', () => {
    const result = nsTransport.idtltTransport.validate({
      _type: 'BusLine',
      id: 1,
      name: 'One',
      stopIds: []
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(transport2)

      expect(nsTransport.isTransport(result.value)).toBe(true)
      expect(nsTrainLine.isTrainLine(result.value)).toBe(false)
      expect(nsBusLine.isBusLine(result.value)).toBe(true)
    }
  })

  it('should be validated for transport3/busLine2', () => {
    const result = nsTransport.idtltTransport.validate({
      _type: 'BusLine',
      id: 2,
      name: 'Two',
      stopIds: [ 'start1', 'end2' ]
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(transport3)

      expect(nsTransport.isTransport(result.value)).toBe(true)
      expect(nsTrainLine.isTrainLine(result.value)).toBe(false)
      expect(nsBusLine.isBusLine(result.value)).toBe(true)
    }
  })
})
