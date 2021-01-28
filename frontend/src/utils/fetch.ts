import type { ValidationError, Validator } from "idonttrustlikethat";
import { Err, Ok, Result } from "space-lift";

/**
 * Thin wrapper around fetch() that ensures type safety via body validators and explicitly typed error results.
 */
export async function vfetch<VS extends ValidatorMap>(
  input: RequestInfo,
  validators: VS,
  init?: RequestInit
): Promise<FetchResult<VS>> {
  try {
    const response = await fetch(input, init);

    if (response.ok) {
      // if you want it, you gotta validate it.
      if (!validators.ok) return Ok(undefined as any);

      const json = await response.json();
      const body = validators.ok.validate(json);

      if (body.ok) return Ok(body.value as any);
      else return Err({ code: "validationError", errors: body.errors });
    }

    // Only parse the body of status: 400 errors, if applicable.
    if (response.status === 400 && validators[400]) {
      const json = await response.json();
      const body = validators[400].validate(json);

      // If we couldn't parse the body of a 400, simply downgrade the error to a basic 'httpError'
      if (body.ok) {
        return Err({ code: "badRequest", body: body.value as any });
      }
    }

    // Otherwise return a generic httpError
    return Err({ code: "httpError", status: response.status });
  } catch (err: unknown) {
    if (isAbortError(err)) return Err({ code: "aborted" });

    return Err({ code: "unknown", cause: err });
  }
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}

export type VFetchResult<DATA, BADREQUESTBODY = unknown> = Result<
  DATA,
  FetchError<BADREQUESTBODY>
>;

type ValidatorMap = {
  ok?: Validator<unknown>;
  400?: Validator<unknown>;
};

type FetchError<BADREQUESTBODY> =
  /** A bad request with its body validated */
  | { code: "badRequest"; body: BADREQUESTBODY }
  /** Any other http error, or a badly validated 400 */
  | { code: "httpError"; status: number }
  /** An Ok response with validation errors */
  | { code: "validationError"; errors: ValidationError[] }
  /** A client-aborted request */
  | { code: "aborted" }
  /** Any other error */
  | { code: "unknown"; cause: unknown };

type TypeOfValidatorInMap<
  MAP extends ValidatorMap,
  KEY extends keyof ValidatorMap
> = MAP[KEY] extends Validator<unknown> ? MAP[KEY]["T"] : undefined;

type FetchResult<MAP extends ValidatorMap> = Result<
  TypeOfValidatorInMap<MAP, "ok">,
  FetchError<TypeOfValidatorInMap<MAP, 400>>
>;
