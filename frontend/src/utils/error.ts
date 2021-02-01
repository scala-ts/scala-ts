export type Error = {
  error?: string;
  details?: string;
};

export function isError(e: any): e is Error {
  return e && e.error && typeof (e.error == "string") && e.details;
}
