const madge = require('madge')

/**
 * Script that checks for circular dependencies in the project.
 */

// Safelist for circular dependencies we acknowledge as non-preventable.
const allowedCircularDependencies = [
  // ['src/moduleA.ts', 'src/moduleB.ts']
]

function depsEqual(a, b) {
  if (a.length != b.length) return false

  const setA = new Set(a)
  return b.every(dep => setA.has(dep))
}

function isUnknownCircularDep(circularDep) {
  return allowedCircularDependencies.every(
    allowedDeps => !depsEqual(allowedDeps, circularDep)
  )
}

madge('./src/main.ts', {
  tsConfig: './tsconfig.json',
  detectiveOptions: {
    ts: {
      skipTypeImports: true,
    },
    tsx: {
      skipTypeImports: true,
    },
  },
})
  .then(result => {
    const circularDeps = result.circular().filter(isUnknownCircularDep)
    const count = circularDeps.length

    if (count !== 0) {
      console.error(`${count} circular dependencies found`, circularDeps)
      throw new Error('The build contains circular dependencies.')
    }
  })
  .catch(err => {
    console.error(`Error: `, err)
    process.exit(1)
  })
