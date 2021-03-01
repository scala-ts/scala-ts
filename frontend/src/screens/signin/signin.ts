import { errorDebugString } from "idonttrustlikethat";
import { writable, derived, get } from "svelte/store";
import type { Credentials } from "@_generated/Credentials";
import { idtltAuthenticatedUser } from "@_generated/AuthenticatedUser";
import type { ModalProps } from "@components/modal/modal";
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

export const valid = derived(credentials, ($credentials) => !!$credentials);

// Log in
export const pending = writable<boolean>(false);

export const modalStore = writable<ModalProps | undefined>(undefined);

export const loginMessage = writable<
  | {
      level: "warning" | "success";
      text: string;
    }
  | undefined
>(undefined);

export async function login() {
  pending.set(true);

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

  pending.set(false);

  if (isError(resp)) {
    if (resp.error != "forbidden") {
      modalStore.set({
        id: "error-modal",
        title: "Error",
        message: `${resp.error}: ${JSON.stringify(resp.details)}`,
        headerClass: "bg-danger",
        bodyClass: "text-danger",
        closeBtnClass: "btn-danger",
      });
    } else {
      loginMessage.set({
        level: "warning",
        text:
          typeof resp.details == "string"
            ? resp.details
            : JSON.stringify(resp.details),
      });
    }

    return;
  }

  // ---

  const result = idtltAuthenticatedUser.validate(resp);

  if (!result.ok) {
    modalStore.set({
      id: "error-modal",
      title: "Invalid account",
      message: errorDebugString(result.errors),
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    });

    return;
  }

  // ---

  localStorage.setItem("scala-ts-demo.token", result.value.token);

  loginMessage.set({
    level: "success",
    text: "OK",
  });

  location.href = "/profile";
}
