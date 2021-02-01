import { writable, derived, get, Readable } from "svelte/store";
import type { Account } from "@shared/Account";
import type { ContactName } from "@shared/ContactName";

// Overall store
const initialAccount: () => Account = () => ({
  userName: "",
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
  }
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
  }
);

export const hasContact: Readable<boolean> = derived(
  contactName,
  ($contactName) => !!$contactName
);

// Save
export const error = writable<string | undefined>(undefined);

export const lastSavedName = writable<string | undefined>(undefined);

export async function submitSignUp(account: Account) {
  const resp = await fetch(`${appEnv.backendUrl}/user/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      ...account,
      contactName: get(contactName),
    }),
  });

  const json = await resp.json();

  if (json.error && json.details) {
    error.set(`${json.error}: ${json.details}`);
  } else {
    lastSavedName.set(json);

    // Reset contact
    firstName.set(undefined);
    lastName.set(undefined);
    age.set(undefined);

    accountStore.set(initialAccount());
  }
}
