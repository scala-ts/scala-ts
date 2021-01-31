<script lang="ts">
  import { currentRoute } from "@components/router";
  import SignUp from "@screens/signup/signup.svelte";
  import SignIn from "@screens/signin/signin.svelte";

  $: route = $currentRoute;
</script>

<div id="app" class="container-fluid">
  <main>
    {#if route.name === 'signin'}
      <SignIn />
    {:else}
      <SignUp />
    {/if}
  </main>
</div>
