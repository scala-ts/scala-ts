import * as nsTSName from './scalatsName';
import type { TSName } from './scalatsName';

export const dependencyModules = [
  nsTSName,
];

export class TSConstants {
  public readonly _DefaultName: TSName & "default" = nsTSName.TSName("default");

  public readonly _excluded: ReadonlyArray<string> = [ "foo", "bar" ];

  public readonly _filtered: ReadonlyArray<string> = [ ...this._excluded, ...[ "filtered" ]];

  public readonly _list: ReadonlyArray<TSName> = [ ...[ this._DefaultName ], ...[ nsTSName.TSName("test") ]];

  public readonly _seqOfMap: ReadonlyArray<Readonly<Map<TSName, string>>> = [ (() => { const __buf1628682018: Map<TSName, string> = new Map<TSName, string>(); __buf1628682018.set(nsTSName.TSName("lorem"), "lorem"); __buf1628682018.set(this._DefaultName, "ipsum"); return __buf1628682018 })(), (() => { const __buf1628682049: Map<TSName, string> = new Map<TSName, string>(); __buf1628682049.set(nsTSName.TSName("dolor"), "value"); return __buf1628682049 })() ];

  private static instance: TSConstants;

  private constructor() {}

  public static getInstance() {
    if (!TSConstants.instance) {
      TSConstants.instance = new TSConstants();
    }

    return TSConstants.instance;
  }
}

export const TSConstantsInhabitant: TSConstants = TSConstants.getInstance();

export function isTSConstants(v: any): v is TSConstants {
  return (v instanceof TSConstants) && (v === TSConstantsInhabitant);
}

export type TSConstantsSingleton = TSConstants;
