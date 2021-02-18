// Generated by ScalaTS 0.5.6-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';

import * as nsBusLine from './BusLine';
import * as nsTrainLine from './TrainLine';

// Validator for UnionDeclaration Transport
export const idtltTransport = idtlt.union(
  nsBusLine.idtltDiscriminatedBusLine,
  nsTrainLine.idtltDiscriminatedTrainLine);

// Fields are ignored: name

export const idtltDiscriminatedTransport = idtlt.intersection(
  idtltTransport,
  idtlt.object({
    _type: idtlt.literal('Transport')
  })
);

// Deriving TypeScript type from Transport validator
export type Transport = typeof idtltTransport.T;
