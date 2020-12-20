import { Option } from 'space-monad'
// could be useful to import common types

export interface IBar {
  _name: string;
  _age: number;
  _amount: Option<number>;
  _updated: Date;
  _created: number;
}
export class Bar implements IBar {
  constructor(
    public _name: string,
    public _age: number,
    public _amount: Option<number>,
    public _updated: Date,
    public _created: number
  ) {
    this._name = _name;
    this._age = _age;
    this._amount = _amount;
    this._updated = _updated;
    this._created = _created;
  }
}
