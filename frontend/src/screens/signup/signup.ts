import { writable, derived, get, Readable } from "svelte/store";
import type { Account } from "@_generated/Account";
import type { ContactName } from "@_generated/ContactName";
import { UserName } from "@_generated/UserName";
import type { ModalProps } from "@components/modal/modal";
import { Error, isError } from "@utils/error";

// Overall store
export const initialAccount: () => Account = () => ({
  userName: UserName(""),
  password: "",
  usage: "Personal",
  favoriteFoods: [],
});

export const accountStore = writable<Account>(initialAccount());

export const valid: Readable<boolean> = derived(
  accountStore,
  ($accountStore) => {
    return (
      $accountStore.userName.length > 0 && $accountStore.password.length > 0
    );
  },
);

// Contact name
export const firstName = writable<string | undefined>(undefined);
export const lastName = writable<string | undefined>(undefined);
export const age = writable<number | undefined>(undefined);

const contactName: Readable<ContactName | undefined> = derived(
  [firstName, lastName, age],
  ($store) => {
    const [f, l, a] = $store;

    if (f && f.length > 0 && l && l.length > 0 && a && a > 0) {
      return {
        firstName: f,
        lastName: l,
        age: a,
      };
    } else {
      return undefined;
    }
  },
);

export const hasContact: Readable<boolean> = derived(
  contactName,
  ($contactName) => !!$contactName,
);

// Save
export const pending = writable<boolean>(false);

export const modalStore = writable<ModalProps | undefined>(undefined);

export async function submitSignUp(account: Account) {
  pending.set(true);

  const resp: Error | any = await fetch(`${appEnv.backendUrl}/user/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      ...account,
      contact: get(contactName),
    }),
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
    modalStore.set({
      id: "error-modal",
      title: "Error",
      message: `${resp.error}: ${JSON.stringify(resp.details)}`,
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    });
  } else {
    const userName = resp.toString();

    // Reset contact
    firstName.set(undefined);
    lastName.set(undefined);
    age.set(undefined);

    accountStore.set(initialAccount());

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
  }
}
