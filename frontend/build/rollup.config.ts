import type { RollupOptions } from "rollup";
import path from "path";
import chalk from "chalk";
import svelte from "rollup-plugin-svelte";
import resolve from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import replace from "@rollup/plugin-replace";
import alias from "@rollup/plugin-alias";
import copy from "rollup-plugin-copy";
import esbuild from "rollup-plugin-esbuild";
// @ts-ignore
import css from "rollup-plugin-css-asset";
// @ts-ignore
import html from "@rollup/plugin-html";
// @ts-ignore
import livereload from "rollup-plugin-livereload";
import { scss } from "svelte-preprocess";
import { typescript as esbuildSvelteScript } from "svelte-preprocess-esbuild";

import { config } from "dotenv";
import { envValidator } from "./envValidator";
import { errorDebugString } from "idonttrustlikethat";
import { indexTemplate } from "./indexTemplate";

const buildDirectory = path.resolve(__dirname);

const production = !process.env.ROLLUP_WATCH;

const esbuildTarget = production ? "es6" : "es2017";

const appEnv = envValidator.validate({
  isProd: production,
  ...config().parsed,
});

if (!appEnv.ok)
  throw new Error(
    `The app was given the wrong env variables: \n${errorDebugString(
      appEnv.errors
    )}`
  );

const options: RollupOptions = {
  input: "src/main.ts",

  // Note: Only generate a single chunk for now. If you want multiple chunks, watch out for this bug:
  // https://github.com/egoist/rollup-plugin-esbuild/issues/177
  output: {
    name: "App",
    sourcemap: !production,
    format: "iife",
    dir: "dist",
    entryFileNames: production ? "immutable/[name].[hash].js" : "[name].js",
    assetFileNames: production
      ? "immutable/[name].[hash][extname]"
      : "[name][extname]",
    chunkFileNames: production ? "chunk.[name].[hash].js" : "[name].js",
  },

  external: [],

  watch: {
    clearScreen: false,
  },

  onwarn: (warning) => {
    // Skip certain warnings
    if (
      new Set(["THIS_IS_UNDEFINED", "NON_EXISTENT_EXPORT"]).has(
        warning?.code || ""
      )
    )
      return;

    console.warn(warning.message);
  },

  perf: false,

  plugins: [
    replace({
      "process.env.NODE_ENV": JSON.stringify(production ? "production" : "dev"),
      appEnv: JSON.stringify(appEnv.value),
    }),

    resolve({ browser: true, extensions: [".js", ".ts", ".json", ".svelte"] }),

    commonjs(),

    svelte({
      exclude: "node_modules/**/*",
      preprocess: [
        esbuildSvelteScript({ target: esbuildTarget }),
        scss({
          renderSync: true,
          includePaths: ["./src/theme"],
          prependData: '@import "util.scss";',
        }),
      ],
      compilerOptions: {
        // This is a better default. You can disable it on a per component basis when it makes sense.
        immutable: true,
      },
    }),

    esbuild({
      include: /\.ts$/,
      exclude: /node_modules/,
      minify: production,
      target: esbuildTarget,
    }),

    css({ name: "bundle" }),

    copy({
      targets: [{ src: `public/*`, dest: "dist" }],
    }),

    alias({
      entries: [
        {
          find: "@shared",
          replacement: path.resolve(buildDirectory, "../../shared/src"),
        },
        {
          find: /@(.*)/,
          replacement: path.resolve(buildDirectory, `../src/$1`),
        },
      ],
    }),

    html({
      template: ({ files }: any) => {
        const script = (files.js || [])
          .map(({ fileName }: any) => `<script src='/${fileName}'></script>`)
          .join("\n");

        const css = (files.css || [])
          .map(
            ({ fileName }: any) => `<link rel='stylesheet' href='/${fileName}'>`
          )
          .join("\n");

        return indexTemplate({
          script,
          css,
        });
      },
    }),

    !production && devServer(),

    !production &&
      livereload({
        watch: "dist",
        applyCSSLive: false,
      }),
  ],
};

function devServer() {
  let started = false;

  return {
    writeBundle() {
      if (started) return;

      started = true;

      const sirvPort = 5000;

      // sirv's --quiet mode is all or nothing so let's print our own message to know about the port.
      console.log(
        chalk.blue.bold(`Your application is ready at localhost:${sirvPort}`)
      );

      require("child_process").spawn(
        "yarn",
        ["sirv", "dist", "--dev", "--quiet", "--single", "--port", sirvPort],
        {
          stdio: ["ignore", "inherit", "inherit"],
          shell: true,
        }
      );
    },
  };
}

export default options;
