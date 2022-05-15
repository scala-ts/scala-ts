import { Option } from 'space-monad'
// could be useful to import common types

import * as nsTSName from './scalatsName';
import { TSName, isTSName } from './scalatsName';
import * as nsTSTransport from './scalatsTransport';
import { TSTransport, isTSTransport } from './scalatsTransport';

export interface TSBar {
  _name: TSName;
  _age: number;
  _amount: Option<number>;
  _transports: ReadonlyArray<TSTransport>;
  _updated: Date;
  _created: number;
}

export function isTSBar(v: any): v is TSBar {
  return (
    (v['_name'] && isTSName(v['_name'])) &&
    ((typeof v['_age']) === 'number') &&
    (!v['_amount'] || ((typeof v['_amount']) === 'number')) &&
    (Array.isArray(v['_transports']) && v['_transports'].every(elmt => elmt && isTSTransport(elmt))) &&
    (v['_updated'] && (v['_updated'] instanceof Date)) &&
    (v['_created'] && (v['_created'] instanceof Date))
  );
}
