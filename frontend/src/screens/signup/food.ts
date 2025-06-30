import { get, writable, derived, Readable } from "svelte/store";

import type { Food } from "@_generated/Food";
import type { JapaneseSushi } from "@_generated/JapaneseSushi";
import type { Pizza } from "@_generated/Pizza";
import { discriminatedOtherFood } from "@_generated/OtherFood";

import { accountStore } from "./signup";

export type PizzaSushi = JapaneseSushi | Pizza;

export const pizzaOrSushi = writable<PizzaSushi | undefined>(undefined);

export const availableFoods = writable<ReadonlyArray<PizzaSushi>>([
  "pizza",
  "sushi",
]);

function isSelectable(f: any): f is PizzaSushi {
  return f == "pizza" || f == "sushi";
}

export const canSelectFood: Readable<boolean> = derived(
  availableFoods,
  ($availableFoods) => $availableFoods.length > 0,
);

export const selectFood = (food: PizzaSushi | undefined) => {
  if (!food) {
    return;
  }

  // ---

  availableFoods.update((fs) => {
    const upd = fs.filter((f) => f != food);

    if (upd.length > 0 && upd[0]) {
      pizzaOrSushi.set(upd[0]);
    }

    return upd;
  });

  accountStore.update((a) => ({
    ...a,
    favoriteFoods: [...a.favoriteFoods, food],
  }));
};

export const unselectFood = (food: Food) => {
  if (isSelectable(food)) {
    availableFoods.update((fs) => [...fs, food].sort());
  }

  accountStore.update((a) => ({
    ...a,
    favoriteFoods: a.favoriteFoods.filter((f) => f != food),
  }));
};

// Other
export const otherFood = writable<string | undefined>(undefined);

export const canAddFood: Readable<boolean> = derived(
  otherFood,
  ($otherFood) => {
    const account = get(accountStore);

    if (!$otherFood) {
      return false;
    }

    const favFoods = account.favoriteFoods.map((f) => f.toString());

    return favFoods.indexOf($otherFood) == -1;
  },
);

export const addFood = () => {
  otherFood.update((name) => {
    if (!name) {
      return name;
    }

    // ---

    const food = discriminatedOtherFood({ name });

    accountStore.update((a) => ({
      ...a,
      favoriteFoods: [...a.favoriteFoods, food],
    }));

    return undefined;
  });
};
