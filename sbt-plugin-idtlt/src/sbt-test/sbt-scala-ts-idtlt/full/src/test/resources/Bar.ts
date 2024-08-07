// Generated by ScalaTS 0.7.1-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsBar = exports;

import * as nsName from './Name';
import * as nsTransport from './Transport';

export const dependencyModules = [
  nsName,
  nsTransport,
];

// Validator for InterfaceDeclaration Bar
export const idtltBar = idtlt.object({
  name: nsName.idtltName,
  aliases: idtlt.readonlyArray(nsName.idtltName),
  age: idtlt.number,
  amount: idtlt.number.optional(),
  transports: idtlt.arrayAsSet(nsTransport.idtltTransport).map(set => { type extractGeneric<Type> = Type extends Set<infer X> ? X : never; type extracted = extractGeneric<typeof set>; return set as ReadonlySet<extracted> }),
  updated: idtlt.isoDate,
  created: idtlt.isoDate,
  time: idtlt.string,
});

// Deriving TypeScript type from Bar validator
export type Bar = typeof idtltBar.T;

export const idtltDiscriminatedBar = idtlt.intersection(
  idtltBar,
  idtlt.object({
    _type: idtlt.literal('Bar')
  })
);

// Deriving TypeScript type from idtltDiscriminatedBar validator
export type DiscriminatedBar = typeof idtltDiscriminatedBar.T;

export const discriminatedBar: (_: Bar) => DiscriminatedBar = (v: Bar) => ({ _type: 'Bar', ...v });

export function isBar(v: any): v is Bar {
  return (
    (v['name'] && nsName.isName(v['name'])) &&
    (Array.isArray(v['aliases']) && v['aliases'].every(elmt => elmt && nsName.isName(elmt))) &&
    ((typeof v['age']) === 'number') &&
    (!v['amount'] || ((typeof v['amount']) === 'number')) &&
    ((v['transports'] instanceof Set) && Array.from(v['transports']).every(elmt => elmt && nsTransport.isTransport(elmt))) &&
    (v['updated'] && (v['updated'] instanceof Date)) &&
    (v['created'] && (v['created'] instanceof Date)) &&
    ((typeof v['time']) === 'string')
  );
}