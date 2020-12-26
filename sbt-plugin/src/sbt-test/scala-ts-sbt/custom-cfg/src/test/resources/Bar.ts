import { Option } from 'space-monad'
// could be useful to import common types

export interface TSBar {
  _name: string;
  _age: number;
  _amount: Option<number>;
  _transports: ReadonlyArray<TSTransport>;
  _updated: Date;
  _created: number;
}
