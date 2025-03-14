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
    const localDateVariables = new Set()

    const isLocalDateType = (typeAnnotation) => {
      if (!typeAnnotation) return false

      if (typeAnnotation.type === 'TSTypeReference') {
        return typeAnnotation.typeName.name === 'LocalDate'
      }

      if (typeAnnotation.type === 'TSUnionType') {
        return typeAnnotation.types.some(isLocalDateType)
      }

      return false
    }

    return {
      ImportDeclaration(node) {
        if (node.source.value.endsWith('local-date')) {
          hasLocalDateImport = true
          if (node.specifiers[0] && node.specifiers[0].local) {
            localDateIdentifier = node.specifiers[0].local.name
          }
        }
      },
      VariableDeclarator(node) {
        if (
          node.init &&
          node.init.type === 'CallExpression' &&
          node.init.callee.type === 'MemberExpression' &&
          node.init.callee.object.name === localDateIdentifier &&
          node.init.callee.property.name === 'of'
        ) {
          localDateVariables.add(node.id.name)
        }
      },
      TSParameterProperty(node) {
        if (
          node.parameter.type === 'Identifier' &&
          isLocalDateType(node.parameter.typeAnnotation?.typeAnnotation)
        ) {
          localDateVariables.add(node.parameter.name)
        }
      },
      Identifier(node) {
        if (isLocalDateType(node.typeAnnotation?.typeAnnotation)) {
          localDateVariables.add(node.name)
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

        // Check if either operand is a LocalDate instance
        const isLocalDateInstance = (expr) => {
          if (expr.type === 'Identifier') {
            return localDateVariables.has(expr.name)
          }
          return (
            expr.type === 'CallExpression' &&
            expr.callee.type === 'MemberExpression' &&
            expr.callee.object.name === localDateIdentifier &&
            expr.callee.property.name === 'of'
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
