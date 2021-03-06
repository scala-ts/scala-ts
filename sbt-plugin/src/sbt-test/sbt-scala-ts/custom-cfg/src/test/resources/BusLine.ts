import { TSTransport, isTSTransport } from './scalatsTransport';

export interface TSBusLine extends TSTransport {
  _id: number;
  _name: string;
  _stopIds: ReadonlyArray<string>;
}

export function isTSBusLine(v: any): v is TSBusLine {
  return (
    (Array.isArray(v['_stopIds']) && v['_stopIds'].every(elmt => (typeof elmt) === 'string')) &&
    ((typeof v['_name']) === 'string') &&
    ((typeof v['_id']) === 'number')
  );
}
