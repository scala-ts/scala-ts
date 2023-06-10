// Generated by ScalaTS 0.5.17-SNAPSHOT: https://scala-ts.github.io/scala-ts/

declare var exports: any;

export const nsFoo = exports;

import * as nsFeature from './Feature';
import type { Feature } from './Feature';

export const dependencyModules = [
  nsFeature,
];

export class Foo implements Feature {
  private static instance: Foo;

  private constructor() {}

  public static getInstance() {
    if (!Foo.instance) {
      Foo.instance = new Foo();
    }

    return Foo.instance;
  }
}

export const FooInhabitant: Foo = Foo.getInstance();

export function isFoo(v: any): v is Foo {
  return (v instanceof Foo) && (v === FooInhabitant);
}