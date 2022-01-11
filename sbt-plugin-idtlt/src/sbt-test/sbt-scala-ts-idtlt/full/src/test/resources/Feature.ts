// Generated by ScalaTS 0.5.9-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';

import * as nsBarNum from './BarNum';
import * as nsFooLure from './FooLure';

// Validator for UnionDeclaration Feature
export const idtltFeature = idtlt.union(
  nsBarNum.idtltDiscriminatedBarNum,
  nsFooLure.idtltDiscriminatedFooLure);

// Deriving TypeScript type from Feature validator
export type Feature = typeof idtltFeature.T;

export const idtltDiscriminatedFeature = idtlt.intersection(
  idtltFeature,
  idtlt.object({
    _type: idtlt.literal('Feature')
  })
);

// Deriving TypeScript type from idtltDiscriminatedFeature validator
export type DiscriminatedFeature = typeof idtltDiscriminatedFeature.T;

export const idtltFeatureKnownValues: Array<Feature> = [
  'BarNum', 'FooLure'
];

export function isFeature(v: any): v is Feature {
  return (
    nsBarNum.isBarNum(v) ||
    nsFooLure.isFooLure(v)
  );
}

