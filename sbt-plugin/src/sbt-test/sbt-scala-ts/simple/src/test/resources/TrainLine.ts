// Generated by ScalaTS 0.5.11-SNAPSHOT: https://scala-ts.github.io/scala-ts/

import * as nsTransport from './Transport';
import { Transport, isTransport } from './Transport';

export interface TrainLine extends Transport {
  name: string;
  startStationId: string;
  endStationId: string;
}

export function isTrainLine(v: any): v is TrainLine {
  return (
    ((typeof v['name']) === 'string') &&
    ((typeof v['startStationId']) === 'string') &&
    ((typeof v['endStationId']) === 'string')
  );
}
