import { writable, derived, Readable } from "svelte/store";
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
export const contactStore = writable<ContactName>({
  firstName: "",
  lastName: "",
  age: -1,
});

// Save
export const error = writable<string | undefined>(undefined);

export const lastSavedName = writable<string | undefined>(undefined);

export async function submitSignUp(account: Account) {
  const resp = await fetch(`${appEnv.backendUrl}/user/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(account),
  });

  const json = await resp.json();

  if (json.error && json.details) {
    error.set(`${json.error}: ${json.details}`);
  } else {
    lastSavedName.set(json);
    accountStore.set(initialAccount());
  }
}
