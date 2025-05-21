// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function isRelativeLibImport(node) {
  return node.source.type === 'Literal' && node.source.value.includes('./lib-')
}

function toAbsoluteImport(str) {
  return '"' + str.replace(/^(\.\.\/)+/, '').replace(/^\.\//, '') + '"'
}

export default {
  meta: {
    type: 'suggestion',
    docs: {
      description: 'disallow relative imports from lib-*',
      category: 'Best Practices',
      recommended: true
    },
    fixable: 'code',
    schema: []
  },
  create: function (context) {
    return {
      ImportDeclaration(node) {
        if (isRelativeLibImport(node)) {
          context.report({
            node: node.source,
            message: 'Relative `lib-*` imports are not allowed',
            fix: function (fixer) {
              return fixer.replaceText(
                node.source,
                toAbsoluteImport(node.source.value)
              )
            }
          })
        }
      }
    }
  }
}
