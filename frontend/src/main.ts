import { mount } from "svelte";
import App from "./app.svelte";

const app = mount(App, {
  target: document.body,
});

export default app;
