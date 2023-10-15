export type TSName = string & { __tag: 'TSName' };

export function TSName<T extends string>(value: T): TSName & T {
  return value as (TSName & T)
}

export function isTSName(v: any): v is TSName {
  return (typeof v) === 'string';
}
