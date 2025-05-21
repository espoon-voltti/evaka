// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import path from 'node:path'

function isRelativeImport(node) {
  return (
    node.source.type === 'Literal' &&
    typeof node.source.value === 'string' &&
    (node.source.value.startsWith('./') || node.source.value.startsWith('../'))
  )
}

const defaultImport = 'index'
const defaultSuffixes = ['', '.ts', '.tsx', '.js', '.jsx']

function toShortestRelativeImport(importPath, sourceFile) {
  const sourceDir = path.dirname(sourceFile)
  const absoluteTarget = path.resolve(sourceDir, importPath)
  let shortestRelative = path.relative(sourceDir, absoluteTarget)

  if (
    defaultSuffixes.some((suffix) =>
      shortestRelative.endsWith(`/${defaultImport}${suffix}`)
    )
  ) {
    return shortestRelative.split('/').slice(0, -1).join('/') + '/'
  }
  if (!shortestRelative || shortestRelative === '') return './'
  if (!shortestRelative.startsWith('.')) return './' + shortestRelative
  if (shortestRelative.endsWith('.')) return shortestRelative + '/'
  return shortestRelative
}

export default {
  meta: {
    type: 'suggestion',
    docs: {
      description: 'use shortest possible relative imports',
      category: 'Best Practices',
      recommended: false
    },
    fixable: 'code',
    schema: []
  },
  create(context) {
    return {
      ImportDeclaration(node) {
        if (!isRelativeImport(node)) return

        const importPath = node.source.value
        const filename = context.getFilename()

        const shortest = toShortestRelativeImport(importPath, filename)

        if (shortest !== importPath) {
          context.report({
            node: node.source,
            message: `Use shortest relative import path: "${shortest}"`,
            fix(fixer) {
              return fixer.replaceText(node.source, `"${shortest}"`)
            }
          })
        }
      }
    }
  }
}
