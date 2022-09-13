import * as nsTSTransport from './scalatsTransport';
import type { TSTransport } from './scalatsTransport';

export interface TSBusLine extends TSTransport {
  _id: number;
  _name: string;
  _stopIds: ReadonlyArray<string>;
}

export function isTSBusLine(v: any): v is TSBusLine {
  return (
    ((typeof v['_id']) === 'number') &&
    ((typeof v['_name']) === 'string') &&
    (Array.isArray(v['_stopIds']) && v['_stopIds'].every(elmt => (typeof elmt) === 'string'))
  );
}
