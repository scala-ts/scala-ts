<script lang="ts">
  import { currentRoute } from "@components/router";
  import SignUp from "@screens/signup/signup.svelte";
  import SignIn from "@screens/signin/signin.svelte";
  import Profile from "@screens/profile/profile.svelte";

  let token: string | null = null;

  const checkToken = () => {
    token = localStorage.getItem("scala-ts-demo.token");
    return !!token;
  };

  $: route = $currentRoute;
</script>

<div id="app" class="container-fluid">
  <main>
    {#if route.name === 'signin'}
      <SignIn />
    {:else if route.name === 'profile' && checkToken() && token}
      <Profile {token} />
    {:else}
      <SignUp />
    {/if}
  </main>
</div>
