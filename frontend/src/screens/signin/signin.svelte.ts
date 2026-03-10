import { toStore } from "svelte/store";
import type { Credentials } from "@shared/Credentials";
import type { ModalProps } from "@components/modal/modal";
import { isError } from "@utils/error";

let userNameValue = $state<string | undefined>(undefined);
export const userName = toStore(
  () => userNameValue,
  (value) => {
    userNameValue = value;
  }
);

let passwordValue = $state<string | undefined>(undefined);
export const password = toStore(
  () => passwordValue,
  (value) => {
    passwordValue = value;
  }
);

const credentialsValue = $derived.by(() => {
  const u = userNameValue;
  const p = passwordValue;

  const c: Credentials | undefined =
    u && u.length > 0 && p && p.length > 0
      ? { userName: u, password: p }
      : undefined;

  return c;
});

export const valid = toStore(() => !!credentialsValue);

// Log in
let pendingValue = $state(false);
export const pending = toStore(
  () => pendingValue,
  (value) => {
    pendingValue = value;
  }
);

let modalValue = $state<ModalProps | undefined>(undefined);
export const modalStore = toStore(
  () => modalValue,
  (value) => {
    modalValue = value;
  }
);

type LoginMessage =
  | {
      level: "warning" | "success";
      text: string;
    }
  | undefined;

let loginMessageValue = $state<LoginMessage>(undefined);
export const loginMessage = toStore(
  () => loginMessageValue,
  (value) => {
    loginMessageValue = value;
  }
);

export async function login() {
  pendingValue = true;

  const resp: Error | any = await fetch(`${appEnv.backendUrl}/signin`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(credentialsValue),
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

  pendingValue = false;

  if (isError(resp)) {
    if (resp.error != "forbidden") {
      modalValue = {
        id: "error-modal",
        title: "Error",
        message: `${resp.error}: ${JSON.stringify(resp.details)}`,
        headerClass: "bg-danger",
        bodyClass: "text-danger",
        closeBtnClass: "btn-danger",
      };
    } else {
      loginMessageValue = {
        level: "warning",
        text:
          typeof resp.details == "string"
            ? resp.details
            : JSON.stringify(resp.details),
      };
    }
  } else {
    localStorage.setItem("scala-ts-demo.token", resp.toString());

    loginMessageValue = {
      level: "success",
      text: "OK",
    };

    location.href = "/profile";
  }
}
