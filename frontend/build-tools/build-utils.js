// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const fs = require('fs/promises')
const path = require('path')

module.exports = (loggingEnabled = true) => {
  const log = (line) => {
    if (loggingEnabled) {
      console.info(line)
    }
  }

  return {
    resolveCustomizations() {
      const customizations = process.env.EVAKA_CUSTOMIZATIONS || 'espoo'
      const customizationsPath = `lib-customizations/${customizations}`

      log(`Using customizations from ${customizationsPath}`)

      return customizationsPath
    },

    evakaAliasesPlugin(resolveExtensions, customizationsModule, icons) {
      return {
        name: 'evaka-aliases',
        setup(build) {
          build.onResolve({ filter: /Icons/ }, () => ({
            path: path.resolve(__dirname, `../src/lib-icons/${icons}-icons.ts`)
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
    },

    resolveIcons() {
      switch (process.env.ICONS) {
        case 'pro':
          log('Using pro icons (forced)')
          return 'pro'
        case 'free':
          log('Using free icons (forced)')
          return 'free'
        case undefined:
          break
        default:
          throw new Error(
            `Invalid environment variable ICONS=${process.env.ICONS}`
          )
      }

      try {
        require('@fortawesome/pro-light-svg-icons')
        require('@fortawesome/pro-regular-svg-icons')
        require('@fortawesome/pro-solid-svg-icons')
        log('Using pro icons (auto-detected)')
        return 'pro'
      } catch (e) {
        log('Using free icons (fallback)')
        return 'free'
      }
    }
  }
}
