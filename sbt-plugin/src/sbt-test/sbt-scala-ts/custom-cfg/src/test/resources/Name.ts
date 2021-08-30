export type TSName = string & { __tag: 'TSName' };

export function TSName(value: string): TSName {
  return value as TSName
}

export function isTSName(v: any): v is TSName {
  return (typeof v) === 'string';
}
