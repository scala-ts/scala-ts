import { TSName, isTSName } from './scalatsName';

export class TSConstants {
  public _DefaultName: TSName = TSName("default");

  public _excluded: ReadonlyArray<string> = [ "foo", "bar" ];

  public _filtered: ReadonlyArray<string> = [ ...this._excluded, ...[ "filtered" ]];

  public _list: ReadonlyArray<TSName> = [ ...[ this._DefaultName ], ...[ TSName("test") ]];

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
