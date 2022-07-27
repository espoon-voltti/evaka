// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// TODO Fix before going to prod:
// - Use browserslist to determine the esbuild target
// - Upload a release to sentry
//
// Note:
// - esbuild does not support non-ESM code splitting yet
//   (https://github.com/evanw/esbuild/issues/16)

const fs = require('fs/promises')
const path = require('path')

const esbuild = require('esbuild')
const express = require('express')
const proxy = require('express-http-proxy')
const { template, trimEnd } = require('lodash')
const yargs = require('yargs')

const { resolveCustomizations, evakaAliasesPlugin, resolveIcons } =
  require('./build-utils')(process.env.NODE_ENV !== 'test')
const esbuildBabelStyledComponents = require('./esbuild-babel-styled-components')

function resolveSrcdir(project) {
  return path.resolve(__dirname, `../src/${project.name}`)
}

function resolveOutdir(project) {
  return path.resolve(__dirname, `../dist/esbuild/${project.name}`)
}

function findOutputFile(output, outdir, name, suffix) {
  const re = new RegExp(`^${outdir}/(${name}-.*\\.${suffix}$)`)
  for (const key of Object.keys(output)) {
    const match = key.match(re)
    if (match) {
      return match[1]
    }
  }
  return undefined
}

function script(publicPath, fileName) {
  return '<script defer src="' + publicPath + fileName + '"></script>'
}

function stylesheet(publicPath, fileName) {
  return (
    '<link rel="stylesheet" type="text/css" href="' +
    publicPath +
    fileName +
    '">'
  )
}

function favicon(publicPath) {
  return '<link rel="shortcut icon" href="' + publicPath + 'favicon.ico">'
}

async function buildProject(project, config) {
  const { dev, watch, customizationsModule, icons } = config

  const srcdir = resolveSrcdir(project)
  const outdir = resolveOutdir(project)

  const resolveExtensions = ['.js', '.jsx', '.ts', '.tsx', '.json']

  const buildOutput = await esbuild.build({
    entryPoints: [`${srcdir}/index.tsx`],
    entryNames: '[name]-[hash]',
    bundle: true,
    sourcemap: dev,
    minify: !dev,
    resolveExtensions,
    loader: {
      '.ico': 'file',
      '.png': 'file',
      '.svg': 'file',
      '.woff': 'file',
      '.woff2': 'file'
    },
    publicPath: project.publicPath,
    define: {
      'process.env.APP_COMMIT': `'${process.env.APP_COMMIT || 'UNDEFINED'}'`
    },
    plugins: [
      evakaAliasesPlugin(resolveExtensions, customizationsModule, icons),
      esbuildBabelStyledComponents()
    ],
    metafile: true,
    logLevel: 'info',
    color: dev,
    outdir,
    watch: watch
      ? {
          async onRebuild(error, result) {
            if (error) return
            await staticFiles(project, result.metafile.outputs)
            console.log(`${project.name}: Build done`)
          }
        }
      : undefined,
    splitting: false,
    logOverride: {
      'this-is-undefined-in-esm': 'silent'
    }
  })

  const outputs = buildOutput.metafile.outputs
  await staticFiles(project, outputs)
}

async function staticFiles(project, outputs) {
  const { publicPath, name } = project
  const srcdir = resolveSrcdir(project)
  const outdir = resolveOutdir(project)

  const outputPathDir = path.relative(path.resolve(__dirname, '..'), outdir)
  const indexJs = findOutputFile(outputs, outputPathDir, 'index', 'js')
  const indexCss = findOutputFile(outputs, outputPathDir, 'index', 'css')
  if (!indexJs || !indexCss) {
    throw new Error(`No output file for index.js or index.css for ${name}`)
  }

  const indexHtml = template(await fs.readFile(`${srcdir}/index-esbuild.html`))
  const bodyHtml = await fs.readFile('src/body.html')
  await fs.writeFile(
    `${outdir}/index.html`,
    indexHtml({
      assets: [
        favicon(publicPath),
        stylesheet(publicPath, indexCss),
        script(publicPath, indexJs)
      ].join('\n'),
      appBody: bodyHtml
    })
  )
}

async function serve(projects) {
  const app = express()
  app.use(
    '/api/internal',
    proxy(process.env.API_PROXY_URL ?? 'http://localhost:3020', {
      proxyReqPathResolver: ({ originalUrl }) => originalUrl,
      limit: '100mb'
    })
  )
  app.use(
    '/api/application',
    proxy(process.env.API_PROXY_URL ?? 'http://localhost:3010', {
      proxyReqPathResolver: ({ originalUrl }) => originalUrl
    })
  )
  for (const project of projects) {
    const pathPrefix = trimEnd(project.publicPath, '/')
    const outdir = resolveOutdir(project)
    const middleware = express.static(outdir)

    app.use(pathPrefix, middleware)
    app.get(`${pathPrefix}/*`, (req, res, next) => {
      req.url = `${pathPrefix}/index.html`
      next()
    })
    app.use(pathPrefix, middleware)
  }

  const port = 9099
  app.listen(port, () => {
    console.info(`Server started at http://localhost:${port}`)
  })
}

async function main() {
  const projects = [
    { name: 'employee-mobile-frontend', publicPath: '/employee/mobile/' },
    { name: 'employee-frontend', publicPath: '/employee/' },
    { name: 'citizen-frontend', publicPath: '/' }
  ]

  const args = yargs
    .option('dev', {
      describe: 'Make a development build',
      type: 'boolean',
      default: false
    })
    .option('watch', {
      describe: 'Watch for file changes and rebuild',
      type: 'boolean',
      default: false
    })
    .option('serve', {
      describe: 'Serve the result at localhost:9099',
      type: 'boolean',
      default: false
    })
    .option('projects', {
      describe: 'Projects to build',
      type: 'array',
      default: projects.map(({ name }) => name)
    }).argv

  const config = {
    dev: args.dev,
    watch: args.watch,
    customizationsModule: 'src/' + resolveCustomizations(),
    icons: resolveIcons(),
    babel: typeof args.babel === 'undefined' ? !args.dev : args.babel
  }

  console.log(`Building for ${args.dev ? 'development' : 'production'}`)
  for (const project of projects) {
    if (args.projects.includes(project.name)) {
      await buildProject(project, config)
    }
  }

  if (args.serve) {
    serve(projects)
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
