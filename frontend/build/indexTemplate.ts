interface Options {
  script: string;
  css: string;
}

export function indexTemplate(options: Options) {
  const { script, css } = options;

  const appTitle = "Scala-TS demo";
  const appDescription =
    "TypeScript/Svlete frontend interacting with an Scala/Akka HTTP REST API";

  return `
<!DOCTYPE html>
<html lang="fr">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <meta name="description" content="${appDescription}" />
    <title>${appTitle}</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-giJF6kkoqNQ00vy+HMDP7azOuL0xtbfIcaT9wjKHr8RbDVddVHyTfAAsrekwKmP1" crossorigin="anonymous" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.3.0/font/bootstrap-icons.css" />

    <link rel="preconnect" href="https://fonts.gstatic.com" />
    <link href="https://fonts.googleapis.com/css2?family=Ubuntu&display=swap" rel="stylesheet" />

    <link rel="apple-touch-icon" href="/images/apple-touch-icon.png" sizes="180x180">
    <link rel="icon" href="/images/logo-32.png" sizes="32x32" type="image/png" />
    <link rel="icon" href="/images/logo-16.png" sizes="16x16" type="image/png" />
    <link rel="manifest" href="/favicons/manifest.json">

    <link rel="icon" href="/favicons/favicon.ico" />
    <meta name="theme-color" content="#007acc" />

    ${css}
  </head>

  <body>
    <script>var exports={}</script>
    ${script}

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/js/bootstrap.bundle.min.js" integrity="sha384-ygbV9kiqUc6oa4msXn9868pTtWMgiQaeYH7/t7LECLbyPA2x65Kgf80OJFdroafW" crossorigin="anonymous"></script>
  </body>
</html>`;
}
