// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function isIconsImport(node) {
  return node.source.type === 'Literal' && node.source.value === 'Icons'
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      description: 'disallow imports from Icons',
      category: 'Best Practices',
      recommended: true
    },
    fixable: 'code',
    schema: []
  },
  create: function (context) {
    return {
      ImportDeclaration(node) {
        if (isIconsImport(node)) {
          context.report({
            node: node.source,
            message: 'Importing from `Icons` is not allowed',
            fix: function (fixer) {
              return fixer.replaceText(node.source, '"lib-icons"')
            }
          })
        }
      }
    }
  }
}
