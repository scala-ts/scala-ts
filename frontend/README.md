# Svelte Frontend

## Prerequisites

Install `yarn` globally. All other dependencies are local.

## Running the app in dev mode

- Copy paste `.env.example` and rename it to `.env`
- Run `yarn install` to fetch the dependencies
- Run `yarn dev`

The backend URL (for the Akka HTTP base URL) can be set using environment variables, before running `yarn dev`.

```
export BACKEND_URL=http://localhost:9000

yarn dev
```
