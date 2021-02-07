import type { TSTransport } from './scalatsTransport';
import type { TSWeekDay } from './scalatsWeekDay';

export interface TSTrainLine extends TSTransport {
  _name: string;
  _startStationId: string;
  _endStationId: string;
  _serviceDays: ReadonlyArray<TSWeekDay>;
}
