// Generated by ScalaTS 0.5.15-SNAPSHOT: https://scala-ts.github.io/scala-ts/
import * as idtlt from 'idonttrustlikethat';
export const _externalDependencyModules = [idtlt];

declare var exports: any;

export const nsConstants = exports;

import * as nsName from './Name';

export const dependencyModules = [
  nsName,
];

export class Constants {
  public code: number = 1;

  public UnknownName: nsName.Name = nsName.Name("unknown");

  public defaultName: nsName.Name = nsName.Name("default");

  public list: ReadonlyArray<number> = [ this.code, 2 ];

  public readonly dict: { [key: string]: ReadonlyArray<nsName.Name> } = { "specific": [ this.UnknownName, this.defaultName, nsName.Name("*") ], "invalid": [ nsName.Name("failed") ] };

  public excluded: ReadonlyArray<string> = [ "foo", "bar" ];

  public filtered: ReadonlyArray<string> = [ ...this.excluded, ...[ "filtered" ]];

  public names: ReadonlyArray<nsName.Name> = [ ...[ this.UnknownName, this.defaultName ], ...[ nsName.Name("test") ]];

  private static instance: Constants;

  private constructor() {}

  public static getInstance() {
    if (!Constants.instance) {
      Constants.instance = new Constants();
    }

    return Constants.instance;
  }
}

export const ConstantsInhabitant: Constants = Constants.getInstance();

export function isConstants(v: any): v is Constants {
  return (v instanceof Constants) && (v === ConstantsInhabitant);
}

export const idtltConstants =
  idtlt.unknown.and(_unknown => idtlt.Err(
    'Cannot validator instance for singleton Constants'));
