// Generated by ScalaTS 0.5.5-SNAPSHOT: https://scala-ts.github.io/scala-ts/

import type { Transport } from './Transport';

export interface BusLine extends Transport {
  id: number;
  name: string;
  stopIds: ReadonlyArray<string>;
}
