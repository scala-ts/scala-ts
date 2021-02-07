<script lang="ts">
  import { fade } from "svelte/transition";
  import { signOut } from "./profile";

  export let token: string;
  const name = token.substring(0, token.indexOf(":"));
</script>

<style lang="scss">
  :global(body) {
    background-color: var(--bs-light);
  }

  h1 {
    font-family: "Ubuntu";
  }
</style>

<div in:fade={{ duration: 120 }} class="container-fluid">
  <div class="row justify-content-md-center mt-3">
    <div class="col col-md-8 col-lg-6">
      <div class="card">
        <div class="card-header">
          <h1 class="card-title">Profile</h1>
          <a
            class="btn btn-secondary btn-sm"
            href="#signout"
            on:click|preventDefault={signOut}><i
              class="bi bi-arrow-left-circle-fill" />
            Sign out</a>
        </div>

        <div class="card-body">
          <dl class="row">
            <dt class="col-sm-3">Name</dt>
            <dd class="col-sm-9">{name}</dd>
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
              <i class="bi bi-github" />
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
