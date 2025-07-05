<script lang="ts">
  import { fade } from "svelte/transition";
  import { onDestroy } from "svelte";
  import type { Account } from "@_generated/Account";
  import type { ModalProps } from "@components/modal/modal";
  import { UserName } from "@_generated/UserName";
  import { idtltUsageValues } from "@_generated/Usage";

  import {
    type PizzaSushi,
    availableFoods,
    addFood,
    canSelectFood,
    canAddFood,
    otherFood,
    pizzaOrSushi,
    selectFood,
    unselectFood,
  } from "./food";

  import {
    accountStore,
    firstName,
    lastName,
    age,
    hasContact,
    valid,
    submitSignUp,
    modalStore,
    pending,
    initialAccount
  } from "./signup";

  import * as ModalModule from "@components/modal/modal.svelte";
  const Modal = ModalModule.default;

  // Store subscriptions with proper typing from subscription
  let modal: ModalProps | undefined = undefined;
  const unsubModal = modalStore.subscribe(value => modal = value);
  
  // Account store subscription that captures the type from the store 
  // without explicit type annotations
  let account: Account = initialAccount();
  const unsubAccount = accountStore.subscribe(value => account = value);
  
  // Pending subscription
  let isPending = false;
  const unsubPending = pending.subscribe(value => isPending = value);

  // Additional store subscriptions
  let isHasContact = false;
  const unsubHasContact = hasContact.subscribe(value => isHasContact = value);

  let availableFoodsValue: ReadonlyArray<PizzaSushi> = [];
  const unsubAvailableFoods = availableFoods.subscribe(value => availableFoodsValue = value);

  let pizzaOrSushiValue: PizzaSushi | undefined = undefined;
  const unsubPizzaOrSushi = pizzaOrSushi.subscribe(value => pizzaOrSushiValue = value);

  let canSelectFoodValue = false;
  const unsubCanSelectFood = canSelectFood.subscribe(value => canSelectFoodValue = value);

  let canAddFoodValue = false;
  const unsubCanAddFood = canAddFood.subscribe(value => canAddFoodValue = value);

  let otherFoodValue: string | undefined = undefined;
  const unsubOtherFood = otherFood.subscribe(value => otherFoodValue = value);

  let isValid = false;
  const unsubValid = valid.subscribe(value => isValid = value);

  // Input change handlers
  function updateOtherFood(event: Event) {
    const target = event.target as HTMLInputElement;
    otherFood.set(target.value);
  }

  function updatePizzaOrSushi(event: Event) {
    const target = event.target as HTMLSelectElement;
    pizzaOrSushi.set(target.value as PizzaSushi);
  }

  function updateUserName(event: Event) {
    const target = event.target as HTMLInputElement;
    const updatedAccount = {...account};
    updatedAccount.userName = UserName(target.value);
    accountStore.set(updatedAccount);
  }

  function updatePassword(event: Event) {
    const target = event.target as HTMLInputElement;
    const updatedAccount = {...account};
    updatedAccount.password = target.value;
    accountStore.set(updatedAccount);
  }

  function updateFirstName(event: Event) {
    const target = event.target as HTMLInputElement;
    firstName.set(target.value);
    const updatedAccount = {...account};
    if (!updatedAccount.contact) {
      updatedAccount.contact = { firstName: target.value, lastName: "", age: 0 };
    } else {
      updatedAccount.contact = {...updatedAccount.contact, firstName: target.value};
    }
    accountStore.set(updatedAccount);
  }

  function updateLastName(event: Event) {
    const target = event.target as HTMLInputElement;
    lastName.set(target.value);
    const updatedAccount = {...account};
    if (!updatedAccount.contact) {
      updatedAccount.contact = { firstName: "", lastName: target.value, age: 0 };
    } else {
      updatedAccount.contact = {...updatedAccount.contact, lastName: target.value};
    }
    accountStore.set(updatedAccount);
  }

  function updateAge(event: Event) {
    const target = event.target as HTMLInputElement;
    const ageValue = parseInt(target.value) || undefined;
    age.set(ageValue);
    const updatedAccount = {...account};
    if (!updatedAccount.contact) {
      updatedAccount.contact = { firstName: "", lastName: "", age: ageValue || 0 };
    } else {
      updatedAccount.contact = {...updatedAccount.contact, age: ageValue || 0};
    }
    accountStore.set(updatedAccount);
  }

  function updateUsage(event: Event) {
    const target = event.target as HTMLSelectElement;
    const updatedAccount = {...account};
    updatedAccount.usage = target.value as "Personal" | "Professional";
    accountStore.set(updatedAccount);
  }

  const hideModal = () => modalStore.set(undefined);
  
  // Cleanup subscriptions
  onDestroy(() => {
    unsubModal();
    unsubAccount();
    unsubPending();
    unsubHasContact();
    unsubAvailableFoods();
    unsubPizzaOrSushi();
    unsubCanSelectFood();
    unsubCanAddFood();
    unsubOtherFood();
    unsubValid();
  });
</script>

<div transition:fade={{ duration: 120 }} class="container-fluid">
  {#if modal}
    <Modal state={modal} hide={hideModal} />
  {/if}

  {#if isPending}
    <div
      class="modal-backdrop"
      id="pending-backdrop"
      style="background-color:rgba(0, 0, 0, 0.5)"
      transition:fade={{ duration: 120 }}
    >
      <div
        class="container-fluid text-center position-relative"
        style="top:49%"
      >
        <div class="spinner-border text-white align-middle" role="status"></div>
      </div>
    </div>
  {/if}

  <div
    id="header"
    class="fixed-top bg-white fixed-top border-bottom-1 border-secondary"
  >
    <div class="d-flex flex-row mt-2">
      <div class="flex-fill p-2 text-center">
        <a href="https://scala-ts.github.io/scala-ts/" target="_blank">
          <img
            src="/images/logo-medium.png"
            alt="Scala-TS"
            width="64"
            height="64"
          /></a
        >
      </div>
      <h1 class="flex-fill p-4">Please sign up</h1>
    </div>
  </div>

  <div class="row justify-content-md-center">
    <div class="col col-md-8 col-lg-6">
      <form on:submit|preventDefault={() => submitSignUp(account)}>
        <div class="mb-3">
          <label for="userName" class="form-label">Username</label>
          <input
            type="text"
            class="form-control"
            id="userName"
            value={account.userName}
            on:input={updateUserName}
          />
        </div>

        <div class="mb-3">
          <label for="password" class="form-label">Password</label>
          <input
            type="password"
            class="form-control"
            id="password"
            value={account.password}
            on:input={updatePassword}
          />
        </div>

        <div class="mb-3 card">
          <div class="card-body">
            <div class="card-title">
              <span>Contact</span>

              {#if isHasContact}
                <i class="bi bi-check-square text-primary fw-light"></i>
              {:else}<i class="bi bi-square text-muted fw-light"></i>{/if}
            </div>

            <div>
              <label for="firstName" class="form-label">Firstname</label>
              <input
                type="text"
                class="form-control"
                id="firstName"
                value={account.contact?.firstName || ''}
                on:input={updateFirstName}
              />
            </div>

            <div>
              <label for="lastName" class="form-label">Lastname</label>
              <input
                type="text"
                class="form-control"
                id="lastName"
                value={account.contact?.lastName || ''}
                on:input={updateLastName}
              />
            </div>

            <div>
              <label for="age" class="form-label">Age</label>
              <input
                type="number"
                min="0"
                class="form-control"
                id="age"
                value={account.contact?.age || ''}
                on:input={updateAge}
              />
            </div>
          </div>
        </div>

        <div class="mb-3">
          <label for="usage" class="form-label card-title">Usage</label>
          <select
            class="form-select"
            id="usage"
            value={account.usage}
            on:change={updateUsage}
          >
            {#each idtltUsageValues as usage}
              <option value={usage}>{usage}</option>
            {/each}
          </select>
        </div>

        <div class="mb-3 card" style="margin-bottom: 8rem !important">
          <div class="card-body">
            <label for="food" class="form-label card-title">Favorite food</label
            >
            <div class="input-group">
              <select class="form-select" id="food" value={pizzaOrSushiValue} on:change={updatePizzaOrSushi}>
                {#each availableFoodsValue as food}
                  <option value={food}>{food}</option>
                {/each}
              </select>
              <button
                class="btn btn-secondary"
                disabled={!canSelectFoodValue}
                on:click|preventDefault={() => selectFood(pizzaOrSushiValue)}
              >
                <i class="bi bi-plus-circle"></i>
                Add</button
              >
            </div>

            <div class="input-group mt-2">
              <input
                type="text"
                class="form-control"
                value={otherFoodValue || ''}
                placeholder="... or something else"
                on:input={updateOtherFood}
              />
              <button
                class="btn btn-secondary"
                on:click|preventDefault={() => addFood()}
                disabled={!canAddFoodValue}
              >
                <i class="bi bi-plus-circle"></i>
                Add</button
              >
            </div>

            <ul class="list-group mt-2" id="favorite-food">
              {#if account.favoriteFoods.length == 0}
                <p class="text-muted">Nothing you'd like to eat?</p>
              {:else}
                {#each account.favoriteFoods as food}
                  <li class="list-group-item">
                    <a
                      href="#otherFood"
                      aria-label="Unselect food"
                      on:click|preventDefault={() => unselectFood(food)}
                    >
                      <i class="bi bi-dash-circle"></i></a>
                    <span>
                      {#if food == "pizza" || food == "sushi"}
                        {food}
                      {:else}{food.name}{/if}
                    </span>
                  </li>
                {/each}
              {/if}
            </ul>
          </div>
        </div>

        <div id="signup-footer" class="fixed-bottom bg-white">
          <div class="col-md-8 col-lg-6 mx-auto">
            <div class="d-flex justify-content-between mb-4">
              <div class="p-2">
                <button type="submit" disabled={!isValid} class="btn btn-primary"
                  >Submit</button
                >
              </div>

              <div class="p-2">
                <small
                  ><a
                    href="https://scala-ts.github.io/scala-ts/"
                    target="_blank"
                  >
                    Demo by
                    <img
                      src="/images/logo-32.png"
                      alt="Scala-TS"
                      width="32"
                      height="32"
                    />
                  </a></small
                >
              </div>

              <div class="p-2">
                <i class="bi bi-github"></i>
                <small>
                  <a
                    href="https://github.com/scala-ts/scala-ts/tree/demo/akka-http-svlete"
                    target="_blank">See sources</a
                  ></small
                >
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>

<style lang="scss">
  :global(body) {
    background-color: var(--bs-light);
  }

  #header {
    height: 6rem;
  }

  #header + .row {
    margin-top: 8rem !important;
  }

  #header h1 {
    font-family: "Ubuntu";
  }

  #favorite-food {
    height: 8rem;
    overflow-y: auto;
  }

  #signup-footer a {
    text-decoration: none;
    color: #000;
  }
</style>
