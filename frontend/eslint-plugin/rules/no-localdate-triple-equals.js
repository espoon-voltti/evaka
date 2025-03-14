// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Disallow triple equals comparison between LocalDate instances',
      category: 'Possible Errors',
      recommended: true
    },
    schema: [],
    messages: {
      noTripleEquals:
        'Do not use triple equals (===) to compare LocalDate instances. Use isEqual() method instead.'
    }
  },
  create(context) {
    let hasLocalDateImport = false
    let localDateIdentifier = null

    return {
      ImportDeclaration(node) {
        if (node.source.value.endsWith('local-date')) {
          hasLocalDateImport = true
          if (node.specifiers[0] && node.specifiers[0].local) {
            localDateIdentifier = node.specifiers[0].local.name
          }
        }
      },
      BinaryExpression(node) {
        if (
          !hasLocalDateImport ||
          !localDateIdentifier ||
          node.operator !== '==='
        ) {
          return
        }

        // Check if either operand is a LocalDate instance by looking for LocalDate.of() calls
        const isLocalDateInstance = (expr) => {
          return (
            expr.type === 'Identifier' ||
            (expr.type === 'CallExpression' &&
              expr.callee.type === 'MemberExpression' &&
              expr.callee.object.name === localDateIdentifier &&
              expr.callee.property.name === 'of')
          )
        }

        if (isLocalDateInstance(node.left) && isLocalDateInstance(node.right)) {
          context.report({
            node,
            messageId: 'noTripleEquals'
          })
        }
      }
    }
  }
}
