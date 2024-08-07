// Generated by ScalaTS 0.7.1-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsFeature = exports;

import * as nsBarNum from './BarNum';
import * as nsFooLure from './FooLure';

export const dependencyModules = [
  nsBarNum,
  nsFooLure,
];

// Validator for UnionDeclaration FeatureUnion
export const idtltFeatureUnion = idtlt.union(
  nsBarNum.idtltDiscriminatedBarNum,
  nsFooLure.idtltDiscriminatedFooLure);

// Deriving TypeScript type from FeatureUnion validator
export type FeatureUnion = typeof idtltFeatureUnion.T;

export const idtltDiscriminatedFeatureUnion = idtlt.intersection(
  idtltFeatureUnion,
  idtlt.object({
    _type: idtlt.literal('FeatureUnion')
  })
);

// Deriving TypeScript type from idtltDiscriminatedFeatureUnion validator
export type DiscriminatedFeatureUnion = typeof idtltDiscriminatedFeatureUnion.T;

export const FeatureUnionValues = {
  BarNum: nsBarNum.BarNumInhabitant, 
  FooLure: nsFooLure.FooLureInhabitant
} as const;

export type FeatureUnionValuesKey = keyof typeof FeatureUnionValues;

// Aliases for the Union utilities
export const FeatureValues = FeatureUnionValues;

export type FeatureValuesKey = FeatureUnionValuesKey;

export function mapFeatureUnionValues<T>(f: (_k: FeatureUnionValuesKey) => T): Readonly<Record<FeatureUnionValuesKey, T>> {
  return {
    BarNum: f(nsBarNum.BarNumInhabitant), 
    FooLure: f(nsFooLure.FooLureInhabitant)
  }
}

export function mapFeatureValues<T>(f: (_k: FeatureValuesKey) => T): Readonly<Record<FeatureValuesKey, T>> {
  return mapFeatureUnionValues<T>(f);
}

export const FeatureUnionTypes = {
  BarNum: nsBarNum.BarNumInhabitant, 
  FooLure: nsFooLure.FooLureInhabitant
} as const;

export const FeatureUnion = {
  ...FeatureUnionValues,
  ...FeatureUnionTypes
} as const;

export const idtltFeatureUnionKnownValues: ReadonlySet<FeatureUnion> = new Set<FeatureUnion>(Object.values(FeatureUnion) as ReadonlyArray<FeatureUnion>);

export function isFeatureUnion(v: any): v is FeatureUnion {
  return (
    nsBarNum.isBarNum(v) ||
    nsFooLure.isFooLure(v)
  );
}

export const idtltFeatureKnownValues: ReadonlySet<Feature> =
  idtltFeatureUnionKnownValues;

export const Feature = FeatureUnion;

export class FeatureSingleton {
  public readonly FooLure: nsFooLure.FooLureSingleton = nsFooLure.FooLureInhabitant;

  public readonly BarNum: nsBarNum.BarNumSingleton = nsBarNum.BarNumInhabitant;

  private static instance: FeatureSingleton;

  private constructor() {}

  public static getInstance() {
    if (!FeatureSingleton.instance) {
      FeatureSingleton.instance = new FeatureSingleton();
    }

    return FeatureSingleton.instance;
  }
}

export const FeatureSingletonInhabitant: FeatureSingleton = FeatureSingleton.getInstance();

export function isFeatureSingleton(v: any): v is FeatureSingleton {
  return (v instanceof FeatureSingleton) && (v === FeatureSingletonInhabitant);
}

export const idtltFeatureSingleton =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton FeatureSingleton'));

export const FeatureInhabitant = FeatureSingletonInhabitant;


// Validator for CompositeDeclaration Feature
export const idtltFeature = idtltFeatureUnion;

export function isFeature(v: any): v is Feature {
  return isFeatureUnion(v);
}

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

// Workaround for local type references in the same module
type privateFeature = Feature;

namespace nsFeature {
  export type Feature = privateFeature;
}
