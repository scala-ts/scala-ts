// Generated by ScalaTS 0.4.1-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';

// Validator for InterfaceDeclaration BusLine
export const idtltBusLine = idtlt.object({
  stopIds: idtlt.array(idtlt.string),
  name: idtlt.string,
  id: idtlt.number,
});

// Super-type declaration Transport is ignored

// Deriving TypeScript type from BusLine validator
export type BusLine = typeof idtltBusLine.T;
