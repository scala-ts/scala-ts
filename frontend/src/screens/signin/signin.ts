import { writable, derived, get } from "svelte/store";
import type { Credentials } from "@shared/Credentials";
import { isError } from "@utils/error";

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

export async function login() {
  alert(JSON.stringify(get(credentials)));

  // TODO: pending.set(true);

  const resp: Error | any = await fetch(`${appEnv.backendUrl}/signin`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(get(credentials)),
  })
    .then((resp) => resp.json())
    .catch((err) => {
      if (isError(err)) {
        return err;
      }

      // ---

      const reason: string =
        typeof err == "string" ? err.toString() : JSON.stringify(err);

      return {
        error: "unexpected",
        details: reason,
      };
    });

  //TODO:pending.set(false);

  if (isError(resp)) {
    /* TODO:
    modalStore.set({
      id: "error-modal",
      title: "Error",
      message: `${resp.error}: ${JSON.stringify(resp.details)}`,
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    });
    */
    console.log(JSON.stringify(resp));
  } else {
    const userName = resp.toString();

    // Reset contact
    userName.set(undefined);
    password.set(undefined);

    /* TODO
    modalStore.set({
      id: "success-modal",
      title: "Success",
      message: `User '${userName}' is created.`,
      headerClass: "bg-success",
      closeBtnClass: "btn-secondary",
      extraBtn: {
        classname: "btn-success",
        onclick: () => (location.href = "/signin"),
        label: "Sign in...",
      },
    });
    */

    alert("OK");
  }
}
