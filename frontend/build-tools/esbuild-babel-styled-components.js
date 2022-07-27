// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const fs = require('fs/promises')

const babel = require('@babel/core')
const sucrase = require('sucrase')

// This plugin will slow down builds significantly (5-10x) because
// each file has to go through Babel instead of the built-in esbuild
// transpiler/compiler, which is much faster.
module.exports = () => {
  return {
    name: 'babel-styled-components',
    setup(build) {
      build.onLoad({ filter: /\.tsx?$/ }, async (args) => {
        const code = await fs.readFile(args.path, 'utf8')

        // It is faster to use sucrase to transform the TS to JS
        // than to use @babel/plugin-transform-typescript.
        const transpiled = sucrase.transform(code, {
          filePath: args.path,
          transforms: ['typescript', 'jsx']
        })

        const transformed = await babel.transformAsync(transpiled.code, {
          plugins: [
            [
              'babel-plugin-styled-components',
              {
                fileName: false,
                pure: true,
                ssr: true
              }
            ]
          ]
        })

        return {
          contents: transformed.code,
          loader: 'js'
        }
      })
    }
  }
}
