// Generated by ScalaTS 0.5.20-SNAPSHOT: https://scala-ts.github.io/scala-ts/

declare var exports: any;

export const nsWhatever = exports;

export interface Whatever {
  word: string;
}

export function isWhatever(v: any): v is Whatever {
  return (
    ((typeof v['word']) === 'string')
  );
}
