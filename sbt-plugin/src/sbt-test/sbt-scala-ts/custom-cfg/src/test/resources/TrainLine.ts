import type { TSWeekDay } from './scalatsWeekDay';
import type { TSTransport } from './scalatsTransport';

export interface TSTrainLine extends TSTransport {
  _name: string;
  _startStationId: string;
  _endStationId: string;
  _serviceDays: ReadonlyArray<TSWeekDay>;
}
