import * as nsTSName from './scalatsName';
import type { TSName } from './scalatsName';

export class TSConstants {
  public _DefaultName: TSName = nsTSName.TSName("default");

  public _excluded: ReadonlyArray<string> = [ "foo", "bar" ];

  public _filtered: ReadonlyArray<string> = [ ...this._excluded, ...[ "filtered" ]];

  public _list: ReadonlyArray<TSName> = [ ...[ this._DefaultName ], ...[ nsTSName.TSName("test") ]];

  public _seqOfMap: ReadonlyArray<{ [key: TSName]: string }> = [ (() => { const __buf1628682018: { [key: TSName]: string } = {}; __buf1628682018[nsTSName.TSName("lorem")] = "lorem"; __buf1628682018[this._DefaultName] = "ipsum"; return __buf1628682018 })(), (() => { const __buf1628682049: { [key: TSName]: string } = {}; __buf1628682049[nsTSName.TSName("dolor")] = "value"; return __buf1628682049 })() ];

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
