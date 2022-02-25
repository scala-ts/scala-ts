import { TSTransport, isTSTransport } from './scalatsTransport';
import { TSWeekDay, isTSWeekDay } from './scalatsWeekDay';

export interface TSTrainLine extends TSTransport {
  _name: string;
  _startStationId: string;
  _endStationId: string;
  _serviceDays: ReadonlyArray<TSWeekDay>;
}

export function isTSTrainLine(v: any): v is TSTrainLine {
  return (
    ((typeof v['_name']) === 'string') &&
    ((typeof v['_startStationId']) === 'string') &&
    ((typeof v['_endStationId']) === 'string') &&
    (Array.isArray(v['_serviceDays']) && v['_serviceDays'].every(elmt => elmt && isTSWeekDay(elmt)))
  );
}
