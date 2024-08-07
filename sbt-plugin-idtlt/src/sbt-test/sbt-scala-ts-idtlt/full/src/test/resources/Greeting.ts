// Generated by ScalaTS 0.7.1-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsGreeting = exports;

import * as nsBye from './Bye';
import * as nsGoodBye from './GoodBye';
import * as nsHello from './Hello';
import * as nsHi from './Hi';
import * as nsWhatever from './Whatever';

export const dependencyModules = [
  nsBye,
  nsGoodBye,
  nsHello,
  nsHi,
  nsWhatever,
];

// Validator for UnionDeclaration GreetingUnion
export const idtltGreetingUnion = idtlt.union(
  nsBye.idtltDiscriminatedBye,
  nsGoodBye.idtltDiscriminatedGoodBye,
  nsHello.idtltDiscriminatedHello,
  nsHi.idtltDiscriminatedHi,
  nsWhatever.idtltDiscriminatedWhatever);

// Deriving TypeScript type from GreetingUnion validator
export type GreetingUnion = typeof idtltGreetingUnion.T;

export const idtltDiscriminatedGreetingUnion = idtlt.intersection(
  idtltGreetingUnion,
  idtlt.object({
    _type: idtlt.literal('GreetingUnion')
  })
);

// Deriving TypeScript type from idtltDiscriminatedGreetingUnion validator
export type DiscriminatedGreetingUnion = typeof idtltDiscriminatedGreetingUnion.T;

export const GreetingUnionValues = {
  Bye: nsBye.ByeInhabitant, 
  GoodBye: nsGoodBye.GoodByeInhabitant, 
  Hello: nsHello.HelloInhabitant, 
  Hi: nsHi.HiInhabitant
} as const;

export type GreetingUnionValuesKey = keyof typeof GreetingUnionValues;

// Aliases for the Union utilities
export const GreetingValues = GreetingUnionValues;

export type GreetingValuesKey = GreetingUnionValuesKey;

export function mapGreetingUnionValues<T>(f: (_k: GreetingUnionValuesKey) => T): Readonly<Record<GreetingUnionValuesKey, T>> {
  return {
    Bye: f(nsBye.ByeInhabitant), 
    GoodBye: f(nsGoodBye.GoodByeInhabitant), 
    Hello: f(nsHello.HelloInhabitant), 
    Hi: f(nsHi.HiInhabitant)
  }
}

export function mapGreetingValues<T>(f: (_k: GreetingValuesKey) => T): Readonly<Record<GreetingValuesKey, T>> {
  return mapGreetingUnionValues<T>(f);
}

export const GreetingUnionTypes = {
  Bye: nsBye.ByeInhabitant, 
  GoodBye: nsGoodBye.GoodByeInhabitant, 
  Hello: nsHello.HelloInhabitant, 
  Hi: nsHi.HiInhabitant
} as const;

export const GreetingUnion = {
  ...GreetingUnionValues,
  ...GreetingUnionTypes
} as const;

export const idtltGreetingUnionKnownValues: ReadonlySet<GreetingUnion> = new Set<GreetingUnion>(Object.values(GreetingUnion) as ReadonlyArray<GreetingUnion>);

export function isGreetingUnion(v: any): v is GreetingUnion {
  return (
    nsBye.isBye(v) ||
    nsGoodBye.isGoodBye(v) ||
    nsHello.isHello(v) ||
    nsHi.isHi(v) ||
    nsWhatever.isWhatever(v)
  );
}

export const idtltGreetingKnownValues: ReadonlySet<Greeting> =
  idtltGreetingUnionKnownValues;

export const Greeting = GreetingUnion;

export class GreetingSingleton {
  public readonly Hello: nsHello.HelloSingleton = nsHello.HelloInhabitant;

  public readonly GoodBye: nsGoodBye.GoodByeSingleton = nsGoodBye.GoodByeInhabitant;

  public readonly Hi: nsHi.HiSingleton = nsHi.HiInhabitant;

  public readonly Bye: nsBye.ByeSingleton = nsBye.ByeInhabitant;

  public readonly aliases: Readonly<Map<nsGreeting.Greeting, ReadonlySet<nsGreeting.Greeting>>> = (() => { const __buf914534658: Map<nsGreeting.Greeting, ReadonlySet<nsGreeting.Greeting>> = new Map(); __buf914534658.set(this.Hello, new Set([ this.Hi ])); __buf914534658.set(this.GoodBye, new Set([ this.Bye ])); return __buf914534658 })();

  private static instance: GreetingSingleton;

  private constructor() {}

  public static getInstance() {
    if (!GreetingSingleton.instance) {
      GreetingSingleton.instance = new GreetingSingleton();
    }

    return GreetingSingleton.instance;
  }
}

export const GreetingSingletonInhabitant: GreetingSingleton = GreetingSingleton.getInstance();

export function isGreetingSingleton(v: any): v is GreetingSingleton {
  return (v instanceof GreetingSingleton) && (v === GreetingSingletonInhabitant);
}

export const idtltGreetingSingleton =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton GreetingSingleton'));

export const GreetingInhabitant = GreetingSingletonInhabitant;


// Validator for CompositeDeclaration Greeting
export const idtltGreeting = idtltGreetingUnion;

export function isGreeting(v: any): v is Greeting {
  return isGreetingUnion(v);
}

// Deriving TypeScript type from Greeting validator
export type Greeting = typeof idtltGreeting.T;

export const idtltDiscriminatedGreeting = idtlt.intersection(
  idtltGreeting,
  idtlt.object({
    _type: idtlt.literal('Greeting')
  })
);

// Deriving TypeScript type from idtltDiscriminatedGreeting validator
export type DiscriminatedGreeting = typeof idtltDiscriminatedGreeting.T;

// Workaround for local type references in the same module
type privateGreeting = Greeting;

namespace nsGreeting {
  export type Greeting = privateGreeting;
}
