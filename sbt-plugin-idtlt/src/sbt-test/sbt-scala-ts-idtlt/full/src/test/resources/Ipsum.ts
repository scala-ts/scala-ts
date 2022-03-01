// Generated by ScalaTS 0.5.9: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';

// Validator for SingletonDeclaration Ipsum
export const idtltIpsum = idtlt.literal('Ipsum')

// Super-type declaration Category is ignored
export const idtltDiscriminatedIpsum = idtltIpsum;

// Deriving TypeScript type from Ipsum validator
export type Ipsum = typeof idtltIpsum.T;

export const IpsumInhabitant: Ipsum = 'Ipsum';

export function isIpsum(v: any): v is Ipsum {
  return idtltIpsum.validate(v).ok;
}