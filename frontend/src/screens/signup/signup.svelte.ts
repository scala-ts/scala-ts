import { toStore } from "svelte/store";
import type { Account } from "@shared/Account";
import type { ContactName } from "@shared/ContactName";
import type { ModalProps } from "@components/modal/modal";
import type { Error as ApiError } from "@utils/error";
import { isError } from "@utils/error";

// Overall store
const initialAccount: () => Account = () => ({
  userName: "",
  password: "",
  usage: "Personal",
  favoriteFoods: [],
});

let accountValue = $state<Account>(initialAccount());
export const accountStore = toStore(
  () => accountValue,
  (value) => {
    accountValue = value;
  }
);

export const valid = toStore(
  () => accountValue.userName.length > 0 && accountValue.password.length > 0
);

// Contact name
let firstNameValue = $state<string | undefined>(undefined);
let lastNameValue = $state<string | undefined>(undefined);
let ageValue = $state<number | undefined>(undefined);

export const firstName = toStore(
  () => firstNameValue,
  (value) => {
    firstNameValue = value;
  }
);

export const lastName = toStore(
  () => lastNameValue,
  (value) => {
    lastNameValue = value;
  }
);

export const age = toStore(
  () => ageValue,
  (value) => {
    ageValue = value;
  }
);

const contactNameValue = $derived.by(() => {
  const f = firstNameValue;
  const l = lastNameValue;
  const a = ageValue;

  if (f && f.length > 0 && l && l.length > 0 && a && a > 0) {
    const contact: ContactName = {
      firstName: f,
      lastName: l,
      age: a,
    };

    return contact;
  }

  return undefined;
});

export const hasContact = toStore(() => !!contactNameValue);

// Save
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

export async function submitSignUp(account: Account) {
  pendingValue = true;

  const resp: ApiError | any = await fetch(`${appEnv.backendUrl}/user/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      ...account,
      contact: contactNameValue,
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

  pendingValue = false;

  if (isError(resp)) {
    modalValue = {
      id: "error-modal",
      title: "Error",
      message: `${resp.error}: ${JSON.stringify(resp.details)}`,
      headerClass: "bg-danger",
      bodyClass: "text-danger",
      closeBtnClass: "btn-danger",
    };
  } else {
    const userName = resp.toString();

    // Reset contact
    firstNameValue = undefined;
    lastNameValue = undefined;
    ageValue = undefined;

    accountValue = initialAccount();

    modalValue = {
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
    };
  }
}
