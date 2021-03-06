// Custom declaration handling
export interface TSTransport {
  _name: string;
  _additionalField?: string;
}

export function isTSTransport(v: any): v is TSTransport {
  return true // dummy
}
