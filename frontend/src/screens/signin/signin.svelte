<script lang="ts">
  import { fade } from "svelte/transition";
  //import { Data, fetchData } from './data'

  //let data: Data = undefined

  //fetchData().then(d => (data = d))
</script>

<style lang="scss">
  @import "https://getbootstrap.com/docs/5.0/examples/sign-in/signin.css";
</style>

<div in:fade={{ duration: 120 }} class="text-center">
  <main class="form-signin">
    <form>
      <img
        class="mb-4"
        src="/images/logo-medium.png"
        alt="Scala-TS"
        width="72"
        height="72" />

      <h1 class="h3 mb-3 fw-normal">Please sign in</h1>

      <label for="inputEmail" class="visually-hidden">Email address</label>
      <input
        type="email"
        id="inputEmail"
        class="form-control"
        placeholder="Email address"
        required />
      <label for="inputPassword" class="visually-hidden">Password</label>
      <input
        type="password"
        id="inputPassword"
        class="form-control"
        placeholder="Password"
        required />
      <div class="checkbox mb-3">
        <label>
          <input type="checkbox" value="remember-me" />
          Remember me
        </label>
      </div>
      <button class="w-100 btn btn-lg btn-primary" type="submit">Sign in</button>
    </form>
  </main>
</div>
