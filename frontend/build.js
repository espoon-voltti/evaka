// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// TODO Fix before going to prod:
// - Use browserslist to determine the esbuild target
// - Upload a release to sentry
//
// Nice to have:
// - Use babel-plugin-styled-components

/* eslint-disable no-console */

const fs = require('fs/promises')
const path = require('path')

const esbuild = require('esbuild')
const express = require('express')
const proxy = require('express-http-proxy')
const _ = require('lodash')
const yargs = require('yargs')

function resolveCustomizations() {
  const customizations = process.env.EVAKA_CUSTOMIZATIONS || 'espoo'
  const customizationsPath = `src/lib-customizations/${customizations}`
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

function evakaAliasesPlugin(resolveExtensions, customizationsModule, icons) {
  return {
    name: 'evaka-aliases',
    setup(build) {
      build.onResolve({ filter: /Icons/ }, () => ({
        path: path.resolve(__dirname, `src/lib-icons/${icons}-icons.ts`)
      }))

      const customizationsRe = /^@evaka\/customizations\/([^/]+)$/
      build.onResolve({ filter: customizationsRe }, (args) => {
        const moduleName = args.path.match(customizationsRe)[1]
        return {
          path: path.resolve(`${customizationsModule}/${moduleName}.tsx`)
        }
      })

      // Preserve symlinks under the customizations module. This makes it possible to symlink a
      // customizations module to `lib-customizations/<city-name>` and still use symlinks inside the
      // customizations module.
      //
      // The `preserveSymlinks` option of esbuild doesn't seem to be enough if there's a path that has multiple
      // symlinks along it.
      //
      const customizationsModuleDir = path.resolve(customizationsModule)
      build.onResolve({ filter: /^\.\// }, async (args) => {
        if (args.resolveDir === customizationsModuleDir) {
          const base = path.resolve(args.resolveDir, args.path)
          if (path.extname(args.path)) {
            // The path already has an extension
            return { path: base }
          }
          for (const ext of resolveExtensions) {
            try {
              const pathWithExt = `${base}${ext}`
              await fs.access(pathWithExt)
              return { path: pathWithExt }
            } catch (_e) {
              // ignore
            }
          }
          // Fall through if there's no match => esbuild tries to resolve the path itself (and probably fails)
        }
      })
    }
  }
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

  const buildOptions = {
    bundle: true,
    sourcemap: dev,
    minify: !dev,
    resolveExtensions,
    publicPath: project.publicPath,
    define: {
      'process.env.APP_COMMIT': `'${process.env.APP_COMMIT || 'UNDEFINED'}'`
    },
    plugins: [
      evakaAliasesPlugin(resolveExtensions, customizationsModule, icons)
    ],
    logLevel: 'info',
    color: dev
  }

  if (project.serviceWorker) {
    const swContext = await esbuild.context({
      ...buildOptions,
      entryPoints: [`${srcdir}/service-worker.js`],
      entryNames: '[name]',
      outfile: `${outdir}/service-worker.js`
    })
    if (watch) {
      await swContext.watch()
    } else {
      await swContext.rebuild()
    }
  }

  const context = await esbuild.context({
    ...buildOptions,
    entryPoints: [`${srcdir}/index.tsx`],
    entryNames: '[name]-[hash]',
    loader: {
      '.ico': 'file',
      '.png': 'file',
      '.svg': 'file',
      '.woff': 'file',
      '.woff2': 'file'
    },
    metafile: true,
    outdir,
    plugins: [
      ...buildOptions.plugins,
      {
        name: 'evaka-static-files',
        setup(build) {
          build.onEnd(async (result) => {
            if (!result.metafile) return
            await staticFiles(project, result.metafile.outputs)
            console.log(`${project.name}: Build done`)
          })
        }
      }
    ]
  })
  if (watch) {
    await context.watch()
  } else {
    await context.rebuild()
  }
}

async function staticFiles(project, outputs) {
  const { publicPath, name } = project
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

function serve(projects) {
  const app = express()
  app.use(
    '/api/internal',
    proxy(process.env.API_PROXY_URL ?? 'http://localhost:3000', {
      parseReqBody: false,
      proxyReqPathResolver: ({ originalUrl }) => originalUrl
    })
  )
  app.use(
    '/api/application',
    proxy(process.env.API_PROXY_URL ?? 'http://localhost:3000', {
      parseReqBody: false,
      proxyReqPathResolver: ({ originalUrl }) => originalUrl
    })
  )
  for (const project of projects) {
    const pathPrefix = _.trimEnd(project.publicPath, '/')
    const outdir = resolveOutdir(project)
    const middleware = express.static(outdir)

    app.use(pathPrefix, middleware)
    app.get(`${pathPrefix}/*`, (req, _res, next) => {
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
    {
      name: 'employee-mobile-frontend',
      publicPath: '/employee/mobile/',
      serviceWorker: true
    },
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
