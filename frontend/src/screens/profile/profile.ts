import { writable } from "svelte/store";
import { isAccount } from "@shared/Account";
import type { Account } from "@shared/Account";
import type { ModalProps } from "@components/modal/modal";
import type { Error as ApiError } from "@utils/error";
import { isError } from "@utils/error";

export const account = writable<Account | undefined>(undefined);

export const signOut = () => {
  localStorage.removeItem("scala-ts-demo.token");
  location.href = "/signin";
};

export const pending = writable<boolean>(false);

export const modalStore = writable<ModalProps | undefined>(undefined);

export async function load(token: string) {
  pending.set(true);

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
    modalStore.set({
      id: "error-modal",
      title: "Error",
      message: `${resp.error}: ${JSON.stringify(resp.details)}`,
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    });

    return;
  }

  // ---

  pending.set(false);

  if (!isAccount(resp)) {
    modalStore.set({
      id: "error-modal",
      title: "Error",
      message: "Invalid account",
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    });
  } else {
    account.set(resp);
  }
}
