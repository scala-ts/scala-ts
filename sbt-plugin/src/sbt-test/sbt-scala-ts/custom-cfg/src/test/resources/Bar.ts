import { Option } from 'space-monad'
// could be useful to import common types

import { TSTransport, isTSTransport } from './scalatsTransport';

export interface TSBar {
  _name: string;
  _age: number;
  _amount: Option<number>;
  _transports: ReadonlyArray<TSTransport>;
  _updated: Date;
  _created: number;
}

export function isTSBar(v: any): v is TSBar {
  return (
    (v['_created'] && (v['_created'] instanceof Date)) &&
    (v['_updated'] && (v['_updated'] instanceof Date)) &&
    (Array.isArray(v['_transports']) && v['_transports'].every(elmt => elmt && isTSTransport(elmt))) &&
    (!v['_amount'] || ((typeof v['_amount']) === 'number')) &&
    ((typeof v['_age']) === 'number') &&
    ((typeof v['_name']) === 'string')
  );
}
