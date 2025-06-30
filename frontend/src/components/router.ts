import { readable } from "svelte/store";
import { Route, Router } from "typescript-router";

export const router = Router(
  {
    signup: Route("/"),
    signin: Route("/signin"),
    profile: Route("/profile"),
  },
  { onNotFound: (reason) => console.error(reason) },
);

export type AppRouter = typeof router;
export type AppRoute = AppRouter["route"];

export const currentRoute = readable(router.route, (set) => {
  return router.onChange(() => set(router.route));
});
