// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// TODO Fix before going to prod:
// - Use browserslist to determine the esbuild target
// - Upload a release to sentry
//
// Nice to have:
// - Use babel-plugin-styled-components

const fs = require('fs/promises')
const path = require('path')
const esbuild = require('esbuild')
const alias = require('esbuild-plugin-alias')
const express = require('express')
const proxy = require('express-http-proxy')
const yargs = require('yargs')
const _ = require('lodash')

function resolveCustomizations() {
  const customizations = process.env.EVAKA_CUSTOMIZATIONS || 'espoo'
  const customizationsPath = path.resolve(
    __dirname,
    'src/lib-customizations',
    customizations
  )
  console.info(`Using customizations from ${customizationsPath}`)
  return customizationsPath
}

function resolveIcons() {
  switch (process.env.ICONS) {
    case 'pro':
      console.info('Using pro icons (forced)')
      return 'pro'
    case 'free':
      console.info('Using free icons (forced)')
      return 'free'
    case undefined:
      break
    default:
      throw new Error(`Invalid environment variable ICONS=${process.env.ICONS}`)
  }
  try {
    require('@fortawesome/pro-light-svg-icons')
    require('@fortawesome/pro-regular-svg-icons')
    require('@fortawesome/pro-solid-svg-icons')
    console.info('Using pro icons (auto-detected)')
    return 'pro'
  } catch (e) {
    console.info('Using free icons (fallback)')
    return 'free'
  }
}

function resolveSrcdir(project) {
  return `src/${project.name}`
}

function resolveOutdir(project) {
  return `dist/esbuild/${project.name}`
}

function findOutputFile(obj, outdir, name, suffix) {
  const re = new RegExp(`^${outdir}/(${name}-.*\\.${suffix}$)`)
  for (const key of Object.keys(obj)) {
    const match = key.match(re)
    if (match) {
      return match[1]
    }
  }
  return undefined
}

function script(publicPath, fileName) {
  return `<script defer src="${publicPath}${fileName}"></script>`
}

function stylesheet(publicPath, fileName) {
  return `<link rel="stylesheet" type="text/css" href="${publicPath}${fileName}">`
}

function favicon(publicPath) {
  return `<link rel="shortcut icon" href="${publicPath}favicon.ico">`
}

async function buildProject(project, config) {
  const { dev, watch, customizationsModule, icons } = config

  const srcdir = resolveSrcdir(project)
  const outdir = resolveOutdir(project)

  const buildOutput = await esbuild.build({
    entryPoints: [`${srcdir}/index.tsx`],
    entryNames: '[name]-[hash]',
    bundle: true,
    sourcemap: dev,
    minify: !dev,
    resolveExtensions: ['.js', '.jsx', '.ts', '.tsx', '.json'],
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
      alias({
        Icons: path.resolve(__dirname, `src/lib-icons/${icons}-icons.ts`),
        '@evaka/customizations/common': `${customizationsModule}/common.tsx`,
        '@evaka/customizations/citizen': `${customizationsModule}/citizen.tsx`,
        '@evaka/customizations/employee': `${customizationsModule}/employee.tsx`,
        '@evaka/customizations/employeeMobile': `${customizationsModule}/employeeMobile.tsx`
      })
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
      : undefined
  })

  const outputs = buildOutput.metafile.outputs
  await staticFiles(project, outputs)
}

async function staticFiles(project, outputs) {
  const { publicPath } = project
  const srcdir = resolveSrcdir(project)
  const outdir = resolveOutdir(project)

  const indexJs = findOutputFile(outputs, outdir, 'index', 'js')
  const indexCss = findOutputFile(outputs, outdir, 'index', 'css')
  if (!indexJs || !indexCss) {
    throw new Error(`No output file for index.js or index.css for ${name}`)
  }

  const indexHtml = _.template(
    await fs.readFile(`${srcdir}/index-esbuild.html`)
  )
  await fs.writeFile(
    `${outdir}/index.html`,
    indexHtml({
      assets: [
        favicon(publicPath),
        stylesheet(publicPath, indexCss),
        script(publicPath, indexJs)
      ].join('\n')
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
    const pathPrefix = _.trimEnd(project.publicPath, '/')
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
  const args = yargs
    .option('--dev', {
      describe: 'Make a development build',
      type: 'boolean',
      default: false
    })
    .option('--watch', {
      describe: 'Watch for file changes and rebuild',
      type: 'boolean',
      default: false
    })
    .option('--serve', {
      describe: 'Serve the result at localhost:9099',
      type: 'boolean',
      default: false
    }).argv

  const projects = [
    { name: 'employee-mobile-frontend', publicPath: '/employee/mobile/' },
    { name: 'employee-frontend', publicPath: '/employee/' },
    { name: 'citizen-frontend', publicPath: '/' }
  ]
  const config = {
    dev: args.dev,
    watch: args.watch,
    customizationsModule: resolveCustomizations(),
    icons: resolveIcons()
  }

  console.log(`Building for ${args.dev ? 'development' : 'production'}`)
  for (const project of projects) {
    await buildProject(project, config)
  }

  if (args.serve) {
    serve(projects)
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
