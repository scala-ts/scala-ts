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
    <div class="row justify-content-md-center">
      <div class="alert alert-secondary col col-sm-6 col-md-6 col-lg-4">
        <p>
          This is a
          <a
            href="https://scala-ts.github.io/scala-ts/"
            target="_blank">Scala-TS</a>
          demo.
        </p>

        <p>
          <i class="bi bi-github" />
          See
          <a
            href="https://github.com/scala-ts/scala-ts/tree/demo/akka-http-svlete"
            target="_blank">sources</a>
        </p>
      </div>
    </div>
  </footer>
</div>
