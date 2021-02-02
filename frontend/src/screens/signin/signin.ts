import { writable, derived, get } from "svelte/store";
import type { Credentials } from "@shared/Credentials";

export const userName = writable<string | undefined>(undefined);

export const password = writable<string | undefined>(undefined);

const credentials = derived([userName, password], ($values) => {
  const [u, p] = $values;

  const c: Credentials | undefined =
    u && u.length > 0 && p && p.length > 0
      ? { userName: u, password: p }
      : undefined;

  return c;
});

export const isValid = derived(credentials, ($credentials) => !!$credentials);

export const login = () => {
  alert(JSON.stringify(get(credentials)));
};
