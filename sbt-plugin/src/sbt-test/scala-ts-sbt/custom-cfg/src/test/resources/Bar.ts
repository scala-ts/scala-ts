import { Option } from 'space-monad'
// could be useful to import common types

export interface Bar {
  _name: string;
  _age: number;
  _amount: Option<number>;
  _updated: Date;
  _created: number;
}
