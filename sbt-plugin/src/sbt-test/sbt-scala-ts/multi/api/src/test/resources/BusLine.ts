// Generated by ScalaTS 0.5.12-SNAPSHOT: https://scala-ts.github.io/scala-ts/

import * as nsTransport from './Transport';
import { Transport, isTransport } from './Transport';

export interface BusLine extends Transport {
  id: number;
  name: string;
  stopIds: ReadonlyArray<string>;
}

export function isBusLine(v: any): v is BusLine {
  return (
    ((typeof v['id']) === 'number') &&
    ((typeof v['name']) === 'string') &&
    (Array.isArray(v['stopIds']) && v['stopIds'].every(elmt => (typeof elmt) === 'string'))
  );
}
