// Generated by ScalaTS 0.5.12-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsHello = exports;

// Validator for SingletonDeclaration Hello
export const idtltHello = idtlt.literal('Hello')

// Super-type declaration Greeting is ignored
export const idtltDiscriminatedHello = idtltHello;

// Deriving TypeScript type from Hello validator
export type Hello = typeof idtltHello.T;

export const HelloInhabitant: Hello = 'Hello';

export function isHello(v: any): v is Hello {
  return idtltHello.validate(v).ok;
}
