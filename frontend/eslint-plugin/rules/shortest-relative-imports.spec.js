// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const { RuleTester } = require('eslint')

const shortestRelativeImports = require('./shortest-relative-imports')

new RuleTester().run('shortest-relative-imports', shortestRelativeImports, {
  valid: [
    {
      code: 'import x from "foo"',
      filename: __dirname + '/shortest-relative-imports.js'
    },
    {
      code: 'import x from "../"',
      filename: __dirname + '/shortest-relative-imports.js'
    },
    {
      code: 'import x from "./shortest-relative-imports"',
      filename: __dirname + '/shortest-relative-imports.js'
    },
    {
      code: 'import x from "./shortest-relative-imports.js"',
      filename: __dirname + '/shortest-relative-imports.js'
    },
    {
      code: 'import x from "../jest.config"',
      filename: __dirname + '/shortest-relative-imports.js'
    },
    {
      code: 'import x from "../jest.config.ts"',
      filename: __dirname + '/shortest-relative-imports.js'
    }
  ],
  invalid: [
    {
      code: 'import x from "../rules/shortest-relative-imports"',
      output: 'import x from "./shortest-relative-imports"',
      filename: __dirname + '/shortest-relative-imports.js',
      errors: [
        {
          message:
            'Use shortest relative import path: "./shortest-relative-imports"'
        }
      ]
    },
    {
      code: 'import x from "../../eslint-plugin/index"',
      output: 'import x from "../"',
      filename: __dirname + '/shortest-relative-imports.js',
      errors: [{ message: 'Use shortest relative import path: "../"' }]
    },
    {
      code: 'import x from "../../eslint-plugin"',
      output: 'import x from "../"',
      filename: __dirname + '/shortest-relative-imports.js',
      errors: [{ message: 'Use shortest relative import path: "../"' }]
    }
  ]
})
