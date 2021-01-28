import { readable } from "svelte/store";
import { Route, Router } from "typescript-router";

export const router = Router(
  {
    signup: Route("/"),
    signin: Route("/signin"),
    //shapes: Route('/shapes'),
    //shape: Route('/shape/:id', withId),
  },
  { onNotFound: (reason) => console.error(reason) }
);

export type AppRouter = typeof router;
export type AppRoute = AppRouter["route"];

export const currentRoute = readable(router.route, (set) => {
  const stop = router.onChange(() => set(router.route));
  return stop;
});
