// Generated by ScalaTS 0.5.15-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsName = exports;

// Validator for TaggedDeclaration Name
export type Name = string & { __tag: 'Name' };

export function Name(value: string): Name {
  return value as Name;
}

export const idtltName = idtlt.string.tagged<Name>();

export function isName(v: any): v is Name {
  return idtltName.validate(v).ok;
}
