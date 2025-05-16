// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable no-console */

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import * as esbuild from 'esbuild'
import express from 'express'
import proxy from 'express-http-proxy'
import * as vite from 'vite'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const customizationsPath = resolveCustomizationsPath()
const icons = await resolveIcons()

// Adapted from https://vite.dev/guide/ssr.html#setting-up-the-dev-server

async function devServer() {
  const app = express()

  const viteServer = await vite.createServer({
    ...(await createViteConfig()),
    server: { middlewareMode: true },
    appType: 'custom'
  })

  app.use(viteServer.middlewares)

  app.use(
    '/api',
    proxy(process.env.API_PROXY_URL ?? 'http://localhost:3000', {
      parseReqBody: false,
      proxyReqPathResolver: ({ originalUrl }) => originalUrl
    })
  )

  app.use('/employee/mobile', spaRoot(viteServer, 'employee-mobile-frontend'))
  app.use('/employee', spaRoot(viteServer, 'employee-frontend'))
  app.use('/', spaRoot(viteServer, 'citizen-frontend'))

  console.log('Starting dev server at http://localhost:9099')
  app.listen(9099)
}

async function build() {
  // Vite expects the correct directory hierarchy
  fs.mkdirSync('build/employee/mobile', { recursive: true })
  fs.writeFileSync('build/index.html', makeIndexHtml('citizen-frontend'))
  fs.writeFileSync(
    'build/employee/index.html',
    makeIndexHtml('employee-frontend')
  )
  fs.writeFileSync(
    'build/employee/mobile/index.html',
    makeIndexHtml('employee-mobile-frontend')
  )
  await vite.build({
    ...(await createViteConfig()),
    build: {
      outDir: path.resolve(__dirname, 'dist'),
      rollupOptions: {
        input: {
          citizen: path.resolve(__dirname, 'build/index.html'),
          employee: path.resolve(__dirname, 'build/employee/index.html'),
          employeeMobile: path.resolve(
            __dirname,
            'build/employee/mobile/index.html'
          )
        }
      }
    }
  })

  // Output is in dist/build/* => copy to dist/*
  fs.mkdirSync('dist/employee/mobile', { recursive: true })
  fs.copyFileSync('dist/build/index.html', 'dist/index.html')
  fs.copyFileSync('dist/build/employee/index.html', 'dist/employee/index.html')
  fs.copyFileSync(
    'dist/build/employee/mobile/index.html',
    'dist/employee/mobile/index.html'
  )
  fs.rmSync('dist/build', { recursive: true })

  // Build service worker
  await esbuild.build({
    minify: true,
    bundle: true,
    entryPoints: [
      path.resolve(__dirname, 'src/employee-mobile-frontend/service-worker.js')
    ],
    outfile: path.resolve(__dirname, 'dist/employee/mobile/service-worker.js')
  })

  // Copy favicon from customizations
  fs.copyFileSync(
    `${customizationsPath}/assets/favicon.ico`,
    'dist/favicon.ico'
  )
}

function spaRoot(vite, frontendName) {
  return async (req, res, next) => {
    const url = req.originalUrl

    try {
      res
        .status(200)
        .set({ 'Content-Type': 'text/html' })
        .end(await vite.transformIndexHtml(url, makeIndexHtml(frontendName)))
    } catch (e) {
      vite.ssrFixStacktrace(e)
      next(e)
    }
  }
}

function makeIndexHtml(frontendName) {
  const bodyHtml = fs.readFileSync('src/body.html', 'utf-8')
  return `\
  <!DOCTYPE html>
  <html lang="fi">
    <head>
    <meta charset="utf-8" />
    <title>Varhaiskasvatus</title>
  <link rel="shortcut icon" href="/favicon.ico">
  </head>
  <body>
  ${bodyHtml}
  <script type="module" src="/src/${frontendName}/index.tsx"></script>
  </body>
</html>
`
}

async function createViteConfig() {
  return {
    configFile: false,
    resolve: {
      alias: {
        'lib-common': path.resolve(__dirname, 'src/lib-common'),
        'lib-components': path.resolve(__dirname, 'src/lib-components'),
        'lib-customizations': path.resolve(__dirname, 'src/lib-customizations'),
        'lib-icons': path.resolve(__dirname, 'src/lib-icons'),
        '@evaka/customizations': path.resolve(__dirname, customizationsPath),
        Icons: path.resolve(__dirname, `src/lib-icons/${icons}-icons.ts`)
      }
    },
    define: {
      __APP_COMMIT__: JSON.stringify(process.env.APP_COMMIT || 'UNDEFINED')
    }
  }
}

function resolveCustomizationsPath() {
  const customizations = process.env.EVAKA_CUSTOMIZATIONS || 'espoo'
  const customizationsPath = `src/lib-customizations/${customizations}`
  console.log(`Using customizations from ${customizationsPath}`)
  return customizationsPath
}

async function resolveIcons() {
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
    await import('@fortawesome/pro-light-svg-icons')
    await import('@fortawesome/pro-regular-svg-icons')
    await import('@fortawesome/pro-solid-svg-icons')
    console.info('Using pro icons (auto-detected)')
    return 'pro'
  } catch (e) {
    console.info('Using free icons (fallback)')
    return 'free'
  }
}

const command = process.argv[2]
if (command === 'dev') {
  devServer()
} else if (command === 'build') {
  build()
} else {
  console.log(`Usage: node ${path.basename(process.argv[1])} {dev|build}`)
}
