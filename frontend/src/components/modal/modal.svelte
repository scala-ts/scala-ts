<script lang="ts">
  import { fade } from "svelte/transition";
  import type { ModalProps } from "./modal";

  export let state: ModalProps;
  export let hide: () => void;

  const extraBtn = state.extraBtn;
</script>

<div
  class="modal-backdrop"
  style="background-color:rgba(0, 0, 0, 0.75)"
  transition:fade={{ duration: 120 }}
  on:click={hide}
>
  <div
    class="modal"
    tabindex="-1"
    id={state.id}
    role="dialog"
    style="display:block"
  >
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header text-light {state.headerClass}">
          <h5 class="modal-title">{state.title}</h5>
        </div>
        <div class="modal-body {state.bodyClass}">
          <p>{state.message}</p>
        </div>
        <div class="modal-footer border-0">
          <button
            type="button"
            class="btn {state.closeBtnClass}"
            data-dismiss="modal"
            on:click|preventDefault={hide}>Close</button
          >

          {#if extraBtn}
            <button
              type="button"
              class="btn {extraBtn.classname}"
              on:click|preventDefault={() => {
                hide();
                extraBtn.onclick();
              }}>{extraBtn.label}</button
            >
          {/if}
        </div>
      </div>
    </div>
  </div>
</div>
