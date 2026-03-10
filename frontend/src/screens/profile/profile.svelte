<script lang="ts">
  import { fade } from "svelte/transition";
  import {
    account,
    load,
    modalStore,
    pending,
    signOut,
  } from "./profile.svelte.ts";

  let { token }: { token: string } = $props();
  const name = $derived(token.substring(0, token.indexOf(":")));

  $effect(() => {
    void load(token);
  });

  const a = $derived($account);
  const contact = $derived(a ? a.contact : undefined);
  const usage = $derived(a ? a.usage : undefined);
  const foods = $derived(a ? a.favoriteFoods : []);

  // Modal
  import Modal from "@components/modal/modal.svelte";

  const modal = $derived($modalStore);

  const hideModal = () => modalStore.set(undefined);
</script>

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
            onclick={(event) => {
              event.preventDefault();
              signOut();
            }}><i
              class="bi bi-arrow-left-circle-fill"></i>
            Sign out</a>
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
              <i class="bi bi-github"></i>
              <small>
                <a
                  href="https://github.com/scala-ts/scala-ts/tree/demo/akka-http-svlete"
                  target="_blank">See sources</a></small>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
