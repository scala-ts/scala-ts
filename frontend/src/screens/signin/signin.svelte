<script lang="ts">
  import { fade } from "svelte/transition";
  import { userName, password, isValid, login } from "./signin";
</script>

<style lang="scss">
  h1 {
    font-family: "Ubuntu";
  }
</style>

<div in:fade={{ duration: 120 }} class="container-fluid">
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

        <button
          class="mt-4 w-100 btn btn-lg btn-primary"
          disabled={!$isValid}
          type="submit">Sign in</button>
      </form>
    </div>
  </div>
</div>
