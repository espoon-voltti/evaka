// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/* eslint-disable no-console */

import * as path from 'node:path'

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import type { Plugin, UserConfig } from 'vite'

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
    // @ts-expect-error TS2307
    await import('@fortawesome/pro-light-svg-icons')
    // @ts-expect-error TS2307
    await import('@fortawesome/pro-regular-svg-icons')
    // @ts-expect-error TS2307
    await import('@fortawesome/pro-solid-svg-icons')
    console.info('Using pro icons (auto-detected)')
    return 'pro'
  } catch (e) {
    console.info('Using free icons (fallback)')
    return 'free'
  }
}

function serveIndexHtml(): Plugin {
  return {
    name: 'serve-index-html',
    configureServer(server) {
      server.middlewares.use((originalReq, _res, next) => {
        const req = originalReq
        if (
          req.originalUrl === '/employee/mobile' ||
          req.originalUrl?.startsWith('/employee/mobile/')
        ) {
          req.url = '/src/employee-mobile-frontend/index-vite.html'
        } else if (
          req.originalUrl === '/employee' ||
          req.originalUrl?.startsWith('/employee/')
        ) {
          req.url = '/src/employee-frontend/index-vite.html'
        } else if (req.originalUrl === '/') {
          req.url = '/src/citizen-frontend/index-vite.html'
        } else if (
          req.originalUrl !== '/favicon.ico' &&
          !req.originalUrl?.startsWith('/api/') &&
          !req.originalUrl?.startsWith('/src/') &&
          !req.originalUrl?.startsWith('/node_modules/') &&
          !req.originalUrl?.startsWith('/@')
        ) {
          req.url = '/src/citizen-frontend/index-vite.html'
        }
        next()
      })
    }
  }
}

export default defineConfig(async (): Promise<UserConfig> => {
  const customizationsPath = resolveCustomizationsPath()
  const icons = await resolveIcons()

  return {
    plugins: [react(), serveIndexHtml()],
    publicDir: path.resolve(__dirname, `${customizationsPath}/assets`),
    build: {
      outDir: 'dist/bundle',
      assetsInlineLimit: (filePath, content) => {
        // Avoid inlining service-worker.js
        if (filePath.endsWith('/service-worker.js')) return false

        // Otherwise, inline files up to 4 KB (this is the default)
        return content.length <= 4096
      },
      rollupOptions: {
        input: {
          citizen: path.resolve(
            __dirname,
            'src/citizen-frontend/index-vite.html'
          ),
          employee: path.resolve(
            __dirname,
            'src/employee-frontend/index-vite.html'
          ),
          employeeMobile: path.resolve(
            __dirname,
            'src/employee-mobile-frontend/index-vite.html'
          )
        }
      }
    },
    server: {
      port: 9099,
      proxy: {
        '/api': 'http://localhost:3000'
      }
    },
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
      __APP_COMMIT__: JSON.stringify(process.env.APP_COMMIT || 'UNDEFINED'),
      __IS_VITE__: 'true'
    }
  }
})
