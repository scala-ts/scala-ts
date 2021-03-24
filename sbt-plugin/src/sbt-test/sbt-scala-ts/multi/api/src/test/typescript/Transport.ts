import type { Transport } from '../Transport';
import type { TrainLine } from '../TrainLine';
import type { BusLine } from '../BusLine';

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

    expect(result as TrainLine).toEqual(trainLine1)
    expect(result as Transport).toEqual(transport1)
  })

  it('should be asserted for BusLine', () => {
    expect(transport2.name).toBe(busLine1.name)

    const result = {
      id: 2,
      name: 'Bus#1',
      stopIds: [ 'start', 'middle', 'end' ],
    }

    expect(result as BusLine).toEqual(busLine1)
    expect(result as Transport).toEqual(transport2)
  })
})
