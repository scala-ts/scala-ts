// Generated by ScalaTS 0.7.0-SNAPSHOT: https://scala-ts.github.io/scala-ts/

declare var exports: any;

export const nsNamedFeature = exports;

import * as nsFeature from './Feature';
import type { Feature } from './Feature';

export const dependencyModules = [
  nsFeature,
];

export interface NamedFeature {
  name: string;
  feature: Feature;
}

export function isNamedFeature(v: any): v is NamedFeature {
  return (
    ((typeof v['name']) === 'string') &&
    (v['feature'] && nsFeature.isFeature(v['feature']))
  );
}
