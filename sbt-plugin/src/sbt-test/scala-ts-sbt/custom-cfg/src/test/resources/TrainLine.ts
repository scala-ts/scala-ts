import { TSTransport } from './TSTransport';

export interface TSTrainLine extends TSTransport {
  _name: string;
  _startStationId: string;
  _endStationId: string;
}
