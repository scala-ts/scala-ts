// Generated by ScalaTS 0.5.15-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsWords = exports;

import * as nsGreeting from './Greeting';

export const dependencyModules = [
  nsGreeting,
];

export class Words {
  public start: ReadonlyArray<nsGreeting.Greeting> = [ nsGreeting.Greeting.Hello, nsGreeting.Greeting.Hi ];

  private static instance: Words;

  private constructor() {}

  public static getInstance() {
    if (!Words.instance) {
      Words.instance = new Words();
    }

    return Words.instance;
  }
}

export const WordsInhabitant: Words = Words.getInstance();

export function isWords(v: any): v is Words {
  return (v instanceof Words) && (v === WordsInhabitant);
}

export const idtltWords =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton Words'));
