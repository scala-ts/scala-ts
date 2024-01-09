// Generated by ScalaTS 0.5.20-SNAPSHOT: https://scala-ts.github.io/scala-ts/

declare var exports: any;

export const nsLorem = exports;

import * as nsCategory from './Category';
import type { Category } from './Category';

export const dependencyModules = [
  nsCategory,
];

export class Lorem implements Category {
  private static instance: Lorem;

  private constructor() {}

  public static getInstance() {
    if (!Lorem.instance) {
      Lorem.instance = new Lorem();
    }

    return Lorem.instance;
  }
}

export const LoremInhabitant: Lorem = Lorem.getInstance();

export function isLorem(v: any): v is Lorem {
  return (v instanceof Lorem) && (v === LoremInhabitant);
}
