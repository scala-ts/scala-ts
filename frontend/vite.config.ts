import path from "path";
import { fileURLToPath } from "url";

import { defineConfig, loadEnv } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import preprocess from "svelte-preprocess";

import { errorDebugString } from "idonttrustlikethat";

import { envValidator } from "./build/envValidator";

const __filename = fileURLToPath(import.meta.url);
const rootDir = path.dirname(__filename);

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, rootDir, "");

  const appEnv = envValidator.validate({
    isProd: mode === "production",
    backendUrl: env.BACKEND_URL,
  });

  if (!appEnv.ok) {
    throw new Error(
      `The app was given the wrong env variables:\n${errorDebugString(appEnv.errors)}`
    );
  }

  const frontendPort = Number(env.FRONTEND_PORT || "5173");

  return {
    plugins: [
      svelte({
        preprocess: [
          preprocess({
            scss: {
              includePaths: ["./src/theme"],
              prependData: '@use "util.scss" as *;',
            },
          }),
        ],
      }),
    ],
    resolve: {
      alias: [
        {
          find: "@shared",
          replacement: path.resolve(rootDir, "../common/target/scala-ts/src_managed"),
        },
        {
          find: /@(.*)/,
          replacement: path.resolve(rootDir, "src/$1"),
        },
      ],
    },
    define: {
      appEnv: JSON.stringify(appEnv.value),
      "process.env.NODE_ENV": JSON.stringify(
        mode === "production" ? "production" : "dev"
      ),
    },
    server: {
      host: "0.0.0.0",
      port: frontendPort,
    },
    preview: {
      host: "0.0.0.0",
      port: frontendPort,
    },
    build: {
      outDir: "dist",
      sourcemap: mode !== "production",
    },
  };
});
