import * as idtlt from 'idonttrustlikethat'

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
  it('should be validated for trainLine1', () => {
    const result = nsTrainLine.idtltTrainLine.validate({
      name: 'First',
      startStationId: 'start1',
      endStationId: 'end1'
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(trainLine1)
    }
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
  it('should be validated for busLine1', () => {
    const result = nsBusLine.idtltBusLine.validate({
      id: 1,
      name: 'One',
      stopIds: []
    })

    expect(result.ok).toBe(true)

    if (!result.ok) {
      console.log(result.errors)
    } else {
      expect(result.value).toEqual(busLine1)
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
    }
  })
})
