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

  <footer class="footer mt-auto py-3 text-center">
    <div class="alert alert-secondary">
      This is a
      <a
        href="https://scala-ts.github.io/scala-ts/"
        target="_blank">Scala-TS</a>
      demo.
    </div>
  </footer>
</div>
