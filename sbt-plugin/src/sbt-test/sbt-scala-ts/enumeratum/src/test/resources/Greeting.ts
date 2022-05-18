// Generated by ScalaTS 0.5.12-SNAPSHOT: https://scala-ts.github.io/scala-ts/

declare var exports: any;

export const nsGreeting = exports;

import * as nsBye from './Bye';
import type { Bye } from './Bye';
import * as nsGoodBye from './GoodBye';
import type { GoodBye } from './GoodBye';
import * as nsHello from './Hello';
import type { Hello } from './Hello';
import * as nsHi from './Hi';
import type { Hi } from './Hi';
import * as nsWhatever from './Whatever';
import type { Whatever } from './Whatever';

export const dependencyModules = [
  nsBye,
  nsGoodBye,
  nsHello,
  nsHi,
  nsWhatever,
];

export type Greeting = Bye | GoodBye | Hello | Hi | Whatever;

export const Greeting = {
  Bye: nsBye.ByeInhabitant, 
  GoodBye: nsGoodBye.GoodByeInhabitant, 
  Hello: nsHello.HelloInhabitant, 
  Hi: nsHi.HiInhabitant
} as const;

export function isGreeting(v: any): v is Greeting {
  return (
    nsBye.isBye(v) ||
    nsGoodBye.isGoodBye(v) ||
    nsHello.isHello(v) ||
    nsHi.isHi(v) ||
    nsWhatever.isWhatever(v)
  );
}
