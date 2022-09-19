import * as nsTSTransport from './scalatsTransport';
import type { TSTransport } from './scalatsTransport';
import * as nsTSWeekDay from './scalatsWeekDay';
import type { TSWeekDay } from './scalatsWeekDay';

export const dependencyModules = [
  nsTSTransport,
  nsTSWeekDay,
];

export interface TSTrainLine extends TSTransport {
  _name: string;
  _startStationId: string;
  _endStationId: string;
  _serviceDays: ReadonlySet<TSWeekDay>;
}

export function isTSTrainLine(v: any): v is TSTrainLine {
  return (
    ((typeof v['_name']) === 'string') &&
    ((typeof v['_startStationId']) === 'string') &&
    ((typeof v['_endStationId']) === 'string') &&
    ((v['_serviceDays'] instanceof Set) && Array.from(v['_serviceDays']).every(elmt => elmt && nsTSWeekDay.isTSWeekDay(elmt)))
  );
}
