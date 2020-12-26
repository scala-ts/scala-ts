import { TSTransport } from './TSTransport';

export interface TSBusLine extends TSTransport {
  _id: number;
  _name: string;
  _stopIds: ReadonlyArray<string>;
}
