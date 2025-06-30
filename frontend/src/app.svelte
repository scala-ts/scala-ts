<script lang="ts">
  import { currentRoute } from "@components/router";
  import { onDestroy } from "svelte";
  import SignUp from "@screens/signup/signup.svelte";
  import SignIn from "@screens/signin/signin.svelte";
  import Profile from "@screens/profile/profile.svelte";

  // Define type at the top level
  type AppRoute = { name: string };

  let token: string | null = null;

  const checkToken = () => {
    token = localStorage.getItem("scala-ts-demo.token");
    return !!token;
  };

  // Store subscription
  let route: AppRoute;
  const unsubRoute = currentRoute.subscribe(value => route = value);
  
  // Cleanup subscriptions
  onDestroy(() => {
    unsubRoute();
  });
</script>

<div id="app" class="container-fluid">
  <main>
    {#if route.name === "signin"}
      <SignIn />
    {:else if route.name === "profile" && checkToken() && token}
      <Profile {token} />
    {:else}
      <SignUp />
    {/if}
  </main>
</div>
