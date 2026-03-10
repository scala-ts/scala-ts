<script lang="ts">
  import { router } from "@components/router";
  import type { Snippet } from "svelte";
  import type { AppRouter } from "@components/router";
  import type { RouteAndParams } from "typescript-router";

  let {
    ref = undefined,
    route,
    replace = false,
    children,
  }: {
    ref?: string;
    route: RouteAndParams<AppRouter>;
    replace?: boolean;
    children?: Snippet;
  } = $props();

  const href = $derived(router.link(route[0], route[1]));

  let preventClickDefault = $state(false);

  function onMouseDown(evt: MouseEvent) {
    const currentTarget = evt.currentTarget as HTMLAnchorElement | null;

    if (!currentTarget) {
      return;
    }

    const isModifiedEvent = Boolean(
      evt.metaKey || evt.altKey || evt.ctrlKey || evt.shiftKey
    );
    const isSelfTarget =
      !evt.target ||
      !currentTarget.target ||
      currentTarget.target === "_self";

    if (
      isSelfTarget && // Ignore everything but links with target self
      evt.button === 0 && // ignore everything but left clicks
      !isModifiedEvent // ignore clicks with modifier keys
    ) {
      preventClickDefault = true;

      if (replace) router.replace(route[0], route[1]);
      else router.push(route[0], route[1]);
    }
  }

  function onClick(evt: MouseEvent) {
    if (preventClickDefault) {
      preventClickDefault = false;
      evt.preventDefault();
    }
  }
</script>

<a data-ref={ref} {href} onmousedown={onMouseDown} onclick={onClick}>
  {@render children?.()}
</a>
