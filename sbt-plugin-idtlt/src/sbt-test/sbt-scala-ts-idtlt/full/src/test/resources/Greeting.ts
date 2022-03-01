// Generated by ScalaTS 0.5.10-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';

import * as nsBye from './Bye';
import * as nsGoodBye from './GoodBye';
import * as nsHello from './Hello';
import * as nsHi from './Hi';
import * as nsWhatever from './Whatever';

// Validator for UnionDeclaration Greeting
export const idtltGreeting = idtlt.union(
  nsBye.idtltDiscriminatedBye,
  nsGoodBye.idtltDiscriminatedGoodBye,
  nsHello.idtltDiscriminatedHello,
  nsHi.idtltDiscriminatedHi,
  nsWhatever.idtltDiscriminatedWhatever);

// Deriving TypeScript type from Greeting validator
export type Greeting = typeof idtltGreeting.T;

export const idtltDiscriminatedGreeting = idtlt.intersection(
  idtltGreeting,
  idtlt.object({
    _type: idtlt.literal('Greeting')
  })
);

// Deriving TypeScript type from idtltDiscriminatedGreeting validator
export type DiscriminatedGreeting = typeof idtltDiscriminatedGreeting.T;

export const idtltGreetingKnownValues: Array<Greeting> = [
  'Bye', 'GoodBye', 'Hello', 'Hi'
];

export function isGreeting(v: any): v is Greeting {
  return (
    nsBye.isBye(v) ||
    nsGoodBye.isGoodBye(v) ||
    nsHello.isHello(v) ||
    nsHi.isHi(v) ||
    nsWhatever.isWhatever(v)
  );
}
