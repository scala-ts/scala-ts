import { toStore } from "svelte/store";
import { isAccount } from "@shared/Account";
import type { Account } from "@shared/Account";
import type { ModalProps } from "@components/modal/modal";
import type { Error as ApiError } from "@utils/error";
import { isError } from "@utils/error";

let accountValue = $state<Account | undefined>(undefined);
export const account = toStore(
  () => accountValue,
  (value) => {
    accountValue = value;
  }
);

export const signOut = () => {
  localStorage.removeItem("scala-ts-demo.token");
  location.href = "/signin";
};

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

export async function load(token: string) {
  pendingValue = true;

  const resp: ApiError | any = await fetch(`${appEnv.backendUrl}/user/profile`, {
    method: "GET",
    headers: {
      Authorization: `Basic ${btoa(token)}`,
    },
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

  if (isError(resp)) {
    modalValue = {
      id: "error-modal",
      title: "Error",
      message: `${resp.error}: ${JSON.stringify(resp.details)}`,
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    };

    return;
  }

  // ---

  pendingValue = false;

  if (!isAccount(resp)) {
    modalValue = {
      id: "error-modal",
      title: "Error",
      message: "Invalid account",
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    };
  } else {
    accountValue = resp;
  }
}
