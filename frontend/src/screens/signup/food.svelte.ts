import { fromStore, toStore } from "svelte/store";

import type { Food } from "@shared/Food";
import type { JapaneseSushi } from "@shared/JapaneseSushi";
import type { Pizza } from "@shared/Pizza";
import type { OtherFood } from "@shared/OtherFood";

import { accountStore } from "./signup.svelte.ts";

type PizzaSushi = JapaneseSushi | Pizza;

let pizzaOrSushiValue = $state<PizzaSushi | undefined>(undefined);
export const pizzaOrSushi = toStore(
  () => pizzaOrSushiValue,
  (value) => {
    pizzaOrSushiValue = value;
  }
);

let availableFoodsValue = $state<ReadonlyArray<PizzaSushi>>([
  "pizza",
  "sushi",
]);
export const availableFoods = toStore(
  () => availableFoodsValue,
  (value) => {
    availableFoodsValue = value;
  }
);

function isSelectable(f: any): f is PizzaSushi {
  return f == "pizza" || f == "sushi";
}

export const canSelectFood = toStore(() => availableFoodsValue.length > 0);

export const selectFood = (food: PizzaSushi | undefined) => {
  if (!food) {
    return;
  }

  // ---

  const upd = availableFoodsValue.filter((f) => f != food);

  if (upd.length > 0 && upd[0]) {
    pizzaOrSushiValue = upd[0];
  }

  availableFoodsValue = upd;

  accountStore.update((a) => ({
    ...a,
    favoriteFoods: [...a.favoriteFoods, food],
  }));
};

export const unselectFood = (food: Food) => {
  if (isSelectable(food)) {
    availableFoodsValue = [...availableFoodsValue, food].sort();
  }

  accountStore.update((a) => ({
    ...a,
    favoriteFoods: a.favoriteFoods.filter((f) => f != food),
  }));
};

// Other
let otherFoodValue = $state<string | undefined>(undefined);
export const otherFood = toStore(
  () => otherFoodValue,
  (value) => {
    otherFoodValue = value;
  }
);

const account = fromStore(accountStore);

const canAddFoodValue = $derived.by(() => {
  const currentFood = otherFoodValue;
  const currentAccount = account.current;

  if (!currentFood) {
    return false;
  }

  const favFoods = currentAccount.favoriteFoods.map((f) => f.toString());

  return favFoods.indexOf(currentFood) == -1;
});

export const canAddFood = toStore(() => canAddFoodValue);

export const addFood = () => {
  const name = otherFoodValue;

  if (!name) {
    return;
  }

  // ---

  const food: OtherFood = { name };

  accountStore.update((a) => ({
    ...a,
    favoriteFoods: [...a.favoriteFoods, food],
  }));

  otherFoodValue = undefined;
};
