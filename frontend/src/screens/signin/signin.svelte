<script lang="ts">
  import { fade } from "svelte/transition";
  import {
    userName,
    password,
    valid,
    login,
    loginMessage,
    modalStore,
    pending,
  } from "./signin";

  import Modal from "@components/modal/modal.svelte";

  $: modal = $modalStore;
  $: msg = $loginMessage;

  const hideModal = () => modalStore.set(undefined);
</script>

<style lang="scss">
  h1 {
    font-family: "Ubuntu";
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

  <div class="row justify-content-md-center">
    <div class="col col-md-5 col-lg-3">
      <form on:submit|preventDefault={login}>
        <div class="text-center mt-4">
          <img
            class="mb-4"
            src="/images/logo-medium.png"
            alt="Scala-TS"
            width="72"
            height="72" />

          <h1 class="mb-3">Please sign in</h1>
        </div>

        <label for="userName" class="visually-hidden">Email address</label>
        <input
          type="text"
          id="userName"
          class="form-control mb-3"
          placeholder="Username"
          bind:value={$userName} />
        <label for="password" class="visually-hidden">Password</label>
        <input
          type="password"
          id="password"
          class="form-control mb-3"
          placeholder="Password"
          bind:value={$password} />

        {#if msg}
          <div class="alert alert-{msg.level} border-white" role="alert">
            {msg.text}
          </div>
        {/if}

        <button
          class="mt-4 w-100 btn btn-lg btn-primary"
          disabled={!$valid}
          type="submit">Sign in</button>
      </form>
    </div>
  </div>
</div>
