<script lang="ts">
  import { fade } from "svelte/transition";
  import {
    accountStore,
    error,
    lastSavedName,
    valid,
    submitSignUp,
  } from "./controller";
</script>

<div in:fade={{ duration: 120 }} class="container">
  <div class="row justify-content-md-center">
    <div class="col col-md-8 col-lg-6">
      <h1>Please sign up</h1>

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
        <!-- TODO: ContactName { firstName, lastName, age: number } -->
        <!-- TODO: Usage: Personal | Professional -->
        <!-- TODO: favoriteFoods sushi | pizza | ... -->

        <div class="row">
          <div class="col col-md-6 col-lg-6">
            <button
              type="submit"
              disabled={!$valid}
              class="btn btn-primary">Submit</button>
          </div>

          <div class="col col-auto">
            Powered by
            <img
              src="/images/logo-32.png"
              alt="Scala-TS"
              width="32"
              height="32" />
          </div>
        </div>
      </form>

      {#if $lastSavedName}
        <div class="mt-2 alert alert-success">
          User '{$lastSavedName}' created.
        </div>
      {:else if $error}
        <div class="mt-2 alert alert-danger">{$error}</div>
      {/if}
    </div>
  </div>
</div>
