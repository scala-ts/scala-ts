// Generated by ScalaTS 0.5.17-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsBusLine = exports;

// Validator for InterfaceDeclaration BusLine
export const idtltBusLine = idtlt.object({
  id: idtlt.number,
  name: idtlt.string,
  stopIds: idtlt.array(idtlt.string),
});

// Super-type declaration Transport is ignored

// Deriving TypeScript type from BusLine validator
export type BusLine = typeof idtltBusLine.T;

export const idtltDiscriminatedBusLine = idtlt.intersection(
  idtltBusLine,
  idtlt.object({
    _type: idtlt.literal('BusLine')
  })
);

// Deriving TypeScript type from idtltDiscriminatedBusLine validator
export type DiscriminatedBusLine = typeof idtltDiscriminatedBusLine.T;

export const discriminatedBusLine: (_: BusLine) => DiscriminatedBusLine = (v: BusLine) => ({ _type: 'BusLine', ...v });

export function isBusLine(v: any): v is BusLine {
  return (
    ((typeof v['id']) === 'number') &&
    ((typeof v['name']) === 'string') &&
    (Array.isArray(v['stopIds']) && v['stopIds'].every(elmt => (typeof elmt) === 'string'))
  );
}