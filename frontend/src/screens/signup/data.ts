import { writable, derived, Readable } from "svelte/store";
import type { Account } from "@shared/Account";

export const accountStore = writable<Account>({
  userName: "",
  password: "",
  usage: "Personal",
  favoriteFoods: [],
});

export const valid: Readable<boolean> = derived(
  accountStore,
  ($accountStore) => {
    return (
      $accountStore.userName.length > 0 && $accountStore.password.length > 0
    );
  }
);

export async function submitSignUp(account: Account) {
  console.log(`==> ${appEnv.backendUrl}`);
  //export const submitSignUp = (a: Account) => {
  const resp = await fetch(`${appEnv.backendUrl}/user/signup`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(account),
  });

  const json = await resp.json();
  console.log(`data = ${JSON.stringify(json)}`);
}
