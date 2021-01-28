import { object, string, boolean, Ok, Err } from "idonttrustlikethat";

const url = string.flatMap((str) => {
  try {
    new URL(str);
    return Ok(str);
  } catch (err) {
    return Err(`${str} is not a URL`);
  }
});

export const envValidator = object({
  isProd: boolean,
  backendUrl: url.optional().default(""),
});

export type AppEnv = typeof envValidator.T;
