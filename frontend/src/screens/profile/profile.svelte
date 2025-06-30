<script lang="ts">
  import * as idtlt from "idonttrustlikethat";
  import { fade } from "svelte/transition";
  import { onMount, onDestroy } from "svelte";
  import { idtltOtherFood } from "@_generated/OtherFood";
  import { Account } from "@_generated/Account";
  import { account, load, modalStore, pending, signOut } from "./profile";
  import * as ModalModule from "@components/modal/modal.svelte";
  const Modal = ModalModule.default;
  import type { ModalProps } from "@components/modal/modal";

  export let token: string;
  const name = token.substring(0, token.indexOf(":"));

  onMount(async () => load(token));

  // Explicit store subscriptions for Svelte 5
  let a: Account | undefined;
  const unsubAccount = account.subscribe(value => a = value);
  
  $: contact = a?.contact;
  $: usage = a ? a.usage : undefined;
  $: foods = a
    ? a.favoriteFoods.map((f) => {
        return idtlt.is(f, idtltOtherFood) ? f.name : f;
      })
    : [];

  // Modal subscription
  let modal: ModalProps | undefined;
  const unsubModal = modalStore.subscribe(value => modal = value);

  // Pending subscription
  let isPending = false;
  const unsubPending = pending.subscribe(value => {
    // Ensure primitive boolean
    isPending = value === true;
  });

  const hideModal = () => modalStore.set(undefined);
  
  // Cleanup subscriptions
  onDestroy(() => {
    unsubAccount();
    unsubModal();
    unsubPending();
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

  <div class="row justify-content-md-center mt-3">
    <div class="col col-md-8 col-lg-6">
      <div class="card">
        <div class="card-header">
          <h1 class="card-title">Profile</h1>
          <a
            class="btn btn-secondary btn-sm"
            href="#signout"
            on:click|preventDefault={signOut}
            ><i class="bi bi-arrow-left-circle-fill"></i>
            Sign out</a
          >
        </div>

        <div class="card-body">
          <dl class="row">
            <dt class="col-sm-3">Name</dt>
            <dd class="col-sm-9">{name}</dd>
          </dl>

          {#if contact}
            <dl class="row">
              <dt class="col-sm-3">Contact</dt>
              <dd class="col-sm-9">
                {contact.firstName}
                {contact.lastName}
                ({contact.age}
                years)
              </dd>
            </dl>
          {/if}

          {#if usage}
            <dl class="row">
              <dt class="col-sm-3">Usage</dt>
              <dd class="col-sm-9">{usage}</dd>
            </dl>
          {/if}

          <dl class="row">
            <dt class="col-sm-3">Favorite food</dt>
            <dd class="col-sm-9">
              <ul>
                {#each foods as food}
                  <li>{food}</li>
                {/each}
              </ul>
            </dd>
          </dl>
        </div>

        <div class="card-footer">
          <div class="d-flex justify-content-between mb-4">
            <div class="p-2">
              <small
                ><a href="https://scala-ts.github.io/scala-ts/" target="_blank">
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
                  target="_blank">See sources</a
                ></small
              >
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<style lang="scss">
  :global(body) {
    background-color: var(--bs-light);
  }

  h1 {
    font-family: "Ubuntu";
  }

  .card-footer a {
    text-decoration: none;
  }
</style>
