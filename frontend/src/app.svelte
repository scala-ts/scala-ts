<script lang="ts">
  import { currentRoute } from "@components/router";
  import SignUp from "@screens/signup/signup.svelte";
  import SignIn from "@screens/signin/signin.svelte";
  import Profile from "@screens/profile/profile.svelte";

  const route = $derived($currentRoute);

  const token = $derived.by(() => {
    route;
    return localStorage.getItem("scala-ts-demo.token");
  });
</script>

<div id="app" class="container-fluid">
  <main>
    {#if route.name === 'signin'}
      <SignIn />
    {:else if route.name === 'profile' && token}
      <Profile {token} />
    {:else}
      <SignUp />
    {/if}
  </main>
</div>
