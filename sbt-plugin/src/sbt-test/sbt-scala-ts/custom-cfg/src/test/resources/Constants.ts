import { TSName, isTSName } from './scalatsName';

export class TSConstants {
  public DefaultName: TSName = TSName("default");

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
