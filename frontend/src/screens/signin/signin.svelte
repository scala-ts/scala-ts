<script lang="ts">
  // Import statements
  import { fade } from "svelte/transition";
  import { onDestroy } from "svelte";
  import type { ModalProps } from "@components/modal/modal";
  import {
    userName,
    password,
    valid,
    login as loginFn,
    loginMessage,
    modalStore,
    pending,
  } from "./signin";
  import type { UserName } from "@_generated/UserName";
  
  // Explicitly import the modal component with a relative path
  import * as ModalModule from "@components/modal/modal.svelte";
  const Modal = ModalModule.default;

  // Store subscriptions
  let modal: ModalProps | undefined;
  const unsubModal = modalStore.subscribe(value => modal = value);
  
  let msg: { level: "warning" | "success"; text: string; } | undefined;
  const unsubLoginMessage = loginMessage.subscribe(value => msg = value);
  
  let isPending = false;
  const unsubPending = pending.subscribe(value => isPending = value);
  
  let isValid = false;
  const unsubValid = valid.subscribe(value => isValid = value);
  
  // Local variables for form values
  let userNameValue: string | undefined = undefined;
  const unsubUserName = userName.subscribe(value => userNameValue = value);
  
  let passwordValue: string | undefined = undefined;
  const unsubPassword = password.subscribe(value => passwordValue = value);
  
  // Update stores when input changes
  function updateUserName(event: Event) {
    const target = event.target as HTMLInputElement;
    userName.set(target.value as unknown as UserName);
  }
  
  function updatePassword(event: Event) {
    const target = event.target as HTMLInputElement;
    password.set(target.value);
  }

  const hideModal = () => modalStore.set(undefined);
  
  // Submit handler
  const login = () => loginFn();

  const init = (el: HTMLElement) => el.focus();
  
  // Cleanup subscriptions
  onDestroy(() => {
    unsubModal();
    unsubLoginMessage();
    unsubPending();
    unsubValid();
    unsubUserName();
    unsubPassword();
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

  <div class="row justify-content-md-center">
    <div class="col col-md-5 col-lg-3">
      <form on:submit|preventDefault={login}>
        <div class="text-center mt-4">
          <img
            class="mb-4"
            src="/images/logo-medium.png"
            alt="Scala-TS"
            width="72"
            height="72"
          />

          <h1 class="mb-3">Please sign in</h1>
        </div>

        <label for="userName" class="visually-hidden">Email address</label>
        <input
          type="text"
          id="userName"
          class="form-control mb-3"
          placeholder="Username"
          use:init
          value={userNameValue}
          on:input={updateUserName}
        />
        <label for="password" class="visually-hidden">Password</label>
        <input
          type="password"
          id="password"
          class="form-control mb-3"
          placeholder="Password"
          value={passwordValue}
          on:input={updatePassword}
        />

        {#if msg}
          <div class="alert alert-{msg.level} border-white" role="alert">
            {msg.text}
          </div>
        {/if}

        <button
          class="mt-4 w-100 btn btn-lg btn-primary"
          disabled={!isValid}
          type="submit">Sign in</button
        >
      </form>
    </div>
  </div>
</div>

<style lang="scss">
  h1 {
    font-family: "Ubuntu";
  }
</style>
