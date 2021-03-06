import { Transport, isTransport } from '../Transport';
import { TrainLine, isTrainLine } from '../TrainLine';
import { BusLine, isBusLine } from '../BusLine';

const trainLine1: TrainLine = {
  name: 'Train#1',
  startStationId: 'id1',
  endStationId: 'id2',
}
const transport1: Transport = trainLine1

const busLine1: BusLine = {
  id: 2,
  name: 'Bus#1',
  stopIds: [ 'start', 'middle', 'end' ],
}
const transport2: Transport = busLine1

describe('Transport', () => {
  it('should be asserted for TrainLine', () => {
    expect(transport1.name).toBe(trainLine1.name)

    const result = {
      name: 'Train#1',
      startStationId: 'id1',
      endStationId: 'id2',
    }

    expect(isTransport(result)).toBe(true)

    const rt: Transport = result

    expect(rt).toEqual(transport1)

    expect(isBusLine(result)).toBe(false)
    expect(isTrainLine(result)).toBe(true)

    const tl: TrainLine = result
    
    expect(tl).toEqual(trainLine1)
  })

  it('should be asserted for BusLine', () => {
    expect(transport2.name).toBe(busLine1.name)

    const result = {
      id: 2,
      name: 'Bus#1',
      stopIds: [ 'start', 'middle', 'end' ],
    }

    expect(isTransport(result)).toBe(true)

    const rt: Transport = result

    expect(rt).toEqual(transport2)

    expect(isTrainLine(result)).toBe(false)
    expect(isBusLine(result)).toBe(true)

    const bl: BusLine = result

    expect(bl).toEqual(busLine1)
  })
})
