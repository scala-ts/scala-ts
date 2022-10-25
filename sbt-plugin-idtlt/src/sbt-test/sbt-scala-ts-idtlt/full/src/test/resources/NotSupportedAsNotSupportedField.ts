// Generated by ScalaTS 0.5.15: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsNotSupportedAsNotSupportedField = exports;

import * as nsNotSupportedClassAsTypeArgs from './NotSupportedClassAsTypeArgs';

export const dependencyModules = [
  nsNotSupportedClassAsTypeArgs,
];

// Validator for InterfaceDeclaration NotSupportedAsNotSupportedField
export const idtltNotSupportedAsNotSupportedField = idtlt.object({
  notSupportedClassAsTypeArgs: idtlt.unknown /* Unsupported 'NotSupportedClassAsTypeArgs'; Type parameters: idtlt.number */,
});

// Deriving TypeScript type from NotSupportedAsNotSupportedField validator
export type NotSupportedAsNotSupportedField = typeof idtltNotSupportedAsNotSupportedField.T;

export const idtltDiscriminatedNotSupportedAsNotSupportedField = idtlt.intersection(
  idtltNotSupportedAsNotSupportedField,
  idtlt.object({
    _type: idtlt.literal('NotSupportedAsNotSupportedField')
  })
);

// Deriving TypeScript type from idtltDiscriminatedNotSupportedAsNotSupportedField validator
export type DiscriminatedNotSupportedAsNotSupportedField = typeof idtltDiscriminatedNotSupportedAsNotSupportedField.T;

export const discriminatedNotSupportedAsNotSupportedField: (_: NotSupportedAsNotSupportedField) => DiscriminatedNotSupportedAsNotSupportedField = (v: NotSupportedAsNotSupportedField) => ({ _type: 'NotSupportedAsNotSupportedField', ...v });

export function isNotSupportedAsNotSupportedField(v: any): v is NotSupportedAsNotSupportedField {
  return (
    (v['notSupportedClassAsTypeArgs'] && nsNotSupportedClassAsTypeArgs.isNotSupportedClassAsTypeArgs(v['notSupportedClassAsTypeArgs']))
  );
}
