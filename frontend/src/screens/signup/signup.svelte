<script lang="ts">
  import { fade } from "svelte/transition";
  import { accountStore, valid, submitSignUp } from "./data";
</script>

<div in:fade={{ duration: 120 }} class="container">
  <div class="row justify-content-md-center">
    <div class="col col-lg-6">
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
        <!-- TODO: Other Account properties -->

        <button
          type="submit"
          disabled={!$valid}
          class="btn btn-primary">Submit</button>
      </form>
    </div>
  </div>
</div>
