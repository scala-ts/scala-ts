// Generated by ScalaTS 0.5.15-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsCategory = exports;

import * as nsIpsum from './Ipsum';
import * as nsLorem from './Lorem';

export const dependencyModules = [
  nsIpsum,
  nsLorem,
];

// Validator for UnionDeclaration Category
export const idtltCategory = idtlt.union(
  nsIpsum.idtltDiscriminatedIpsum,
  nsLorem.idtltDiscriminatedLorem);

// Deriving TypeScript type from Category validator
export type Category = typeof idtltCategory.T;

export const idtltDiscriminatedCategory = idtlt.intersection(
  idtltCategory,
  idtlt.object({
    _type: idtlt.literal('Category')
  })
);

// Deriving TypeScript type from idtltDiscriminatedCategory validator
export type DiscriminatedCategory = typeof idtltDiscriminatedCategory.T;

export const Category = {
  Ipsum: nsIpsum.IpsumInhabitant, 
  Lorem: nsLorem.LoremInhabitant
} as const;

export const idtltCategoryKnownValues: ReadonlyArray<Category> = Object.values(Category) as ReadonlyArray<Category>;

export function isCategory(v: any): v is Category {
  return (
    nsIpsum.isIpsum(v) ||
    nsLorem.isLorem(v)
  );
}
