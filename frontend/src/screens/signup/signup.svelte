<script lang="ts">
  import { fade } from "svelte/transition";
  import { UsageValues } from "@shared/Usage";

  import {
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
  } from "./signup";

  import Modal from "@components/modal/modal.svelte";

  $: modal = $modalStore;

  const hideModal = () => modalStore.set(undefined);
</script>

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

<div in:fade={{ duration: 120 }} class="container-fluid">
  {#if modal}
    <Modal state={modal} hide={hideModal} />
  {/if}

  {#if $pending}
    <div
      class="modal-backdrop"
      id="pending-backdrop"
      style="background-color:rgba(0, 0, 0, 0.5)"
      in:fade={{ duration: 120 }}>
      <div
        class="container-fluid text-center position-relative"
        style="top:49%">
        <div class="spinner-border text-white align-middle" role="status" />
      </div>
    </div>
  {/if}

  <div
    id="header"
    class="fixed-top bg-white fixed-top border-bottom-1 border-secondary">
    <div class="d-flex flex-row mt-2">
      <div class="flex-fill p-2 text-center">
        <a href="https://scala-ts.github.io/scala-ts/" target="_blank">
          <img
            src="/images/logo-medium.png"
            alt="Scala-TS"
            width="64"
            height="64" /></a>
      </div>
      <h1 class="flex-fill p-4">Please sign up</h1>
    </div>
  </div>

  <div class="row justify-content-md-center">
    <div class="col col-md-8 col-lg-6">
      <form on:submit|preventDefault={() => submitSignUp($accountStore)}>
        <div class="mb-3">
          <label for="userName" class="form-label">Username</label>
          <input
            type="text"
            class="form-control"
            id="userName"
            bind:value={$accountStore.userName} />
        </div>

        <div class="mb-3">
          <label for="password" class="form-label">Password</label>
          <input
            type="password"
            class="form-control"
            id="password"
            bind:value={$accountStore.password} />
        </div>

        <div class="mb-3 card">
          <div class="card-body">
            <div class="card-title">
              <span>Contact</span>

              {#if $hasContact}
                <i class="bi bi-check-square text-primary fw-light" />
              {:else}<i class="bi bi-square text-muted fw-light" />{/if}
            </div>

            <div>
              <label for="firstName" class="form-label">Firstname</label>
              <input
                type="text"
                class="form-control"
                id="firstName"
                bind:value={$firstName} />
            </div>

            <div>
              <label for="lastName" class="form-label">Lastname</label>
              <input
                type="text"
                class="form-control"
                id="lastName"
                bind:value={$lastName} />
            </div>

            <div>
              <label for="age" class="form-label">Age</label>
              <input
                type="number"
                min="0"
                class="form-control"
                id="age"
                bind:value={$age} />
            </div>
          </div>
        </div>

        <div class="mb-3">
          <label for="usage" class="form-label card-title">Usage</label>
          <select
            class="form-select"
            id="usage"
            bind:value={$accountStore.usage}>
            {#each UsageValues as usage}
              <option value={usage}>{usage}</option>
            {/each}
          </select>
        </div>

        <div class="mb-3 card" style="margin-bottom: 8rem !important">
          <div class="card-body">
            <label for="food" class="form-label card-title">Favorite food</label>
            <div class="input-group">
              <select class="form-select" id="food" bind:value={$pizzaOrSushi}>
                {#each $availableFoods as food}
                  <option value={food}>{food}</option>
                {/each}
              </select>
              <button
                class="btn btn-secondary"
                disabled={!$canSelectFood}
                on:click|preventDefault={() => selectFood($pizzaOrSushi)}>
                <i class="bi bi-plus-circle" />
                Add</button>
            </div>

            <div class="input-group mt-2">
              <input
                type="text"
                class="form-control"
                bind:value={$otherFood}
                placeholder="... or something else" />
              <button
                class="btn btn-secondary"
                on:click|preventDefault={() => addFood()}
                disabled={!$canAddFood}>
                <i class="bi bi-plus-circle" />
                Add</button>
            </div>

            <ul class="list-group mt-2" id="favorite-food">
              {#if $accountStore.favoriteFoods.length == 0}
                <p class="text-muted">Nothing you'd like to eat?</p>
              {:else}
                {#each $accountStore.favoriteFoods as food}
                  <li class="list-group-item">
                    <a
                      href="#otherFood"
                      on:click|preventDefault={() => unselectFood(food)}>
                      <i class="bi bi-dash-circle" /></a>
                    <span>
                      {#if food == 'pizza' || food == 'sushi'}
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
                <button
                  type="submit"
                  disabled={!$valid}
                  class="btn btn-primary">Submit</button>
              </div>

              <div class="p-2">
                <small><a
                    href="https://scala-ts.github.io/scala-ts/"
                    target="_blank">
                    Demo by
                    <img
                      src="/images/logo-32.png"
                      alt="Scala-TS"
                      width="32"
                      height="32" />
                  </a></small>
              </div>

              <div class="p-2">
                <i class="bi bi-github" />
                <small>
                  <a
                    href="https://github.com/scala-ts/scala-ts/tree/demo/akka-http-svlete"
                    target="_blank">See sources</a></small>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>
