// Generated by ScalaTS 0.5.15: https://scala-ts.github.io/scala-ts/

declare var exports: any;

export const nsTrainLine = exports;

import * as nsNamedFeature from './NamedFeature';
import type { NamedFeature } from './NamedFeature';
import * as nsTransport from './Transport';
import type { Transport } from './Transport';

export const dependencyModules = [
  nsNamedFeature,
  nsTransport,
];

export interface TrainLine extends Transport {
  name: string;
  startStationId: string;
  endStationId: string;
  feature: NamedFeature;
}

export function isTrainLine(v: any): v is TrainLine {
  return (
    ((typeof v['name']) === 'string') &&
    ((typeof v['startStationId']) === 'string') &&
    ((typeof v['endStationId']) === 'string') &&
    (v['feature'] && nsNamedFeature.isNamedFeature(v['feature']))
  );
}
