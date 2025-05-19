// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export default {
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

    const isLocalDateTypeAnnotation = (typeAnnotation) => {
      if (!typeAnnotation) return false

      if (typeAnnotation.type === 'TSTypeReference') {
        return typeAnnotation.typeName.name === 'LocalDate'
      }

      if (typeAnnotation.type === 'TSUnionType') {
        return typeAnnotation.types.some(isLocalDateTypeAnnotation)
      }

      return false
    }

    const isLocalDateInstance = (node) => {
      if (!node) return false

      // Check if it's a variable we know is a LocalDate
      if (node.type === 'Identifier' && localDateVariables.has(node.name)) {
        return true
      }

      // Check if it's a direct LocalDate constructor call or static method
      if (
        node.type === 'CallExpression' &&
        node.callee?.type === 'MemberExpression' &&
        node.callee.object?.type === 'Identifier' &&
        node.callee.object.name === localDateIdentifier
      ) {
        return true
      }

      return false
    }

    return {
      ImportDeclaration(node) {
        if (node.source.value.endsWith('local-date')) {
          hasLocalDateImport = true
          if (node.specifiers?.[0]?.local) {
            localDateIdentifier = node.specifiers[0].local.name
          }
        }
      },

      VariableDeclarator(node) {
        // Track variables initialized with LocalDate instances
        if (node.init && isLocalDateInstance(node.init)) {
          localDateVariables.add(node.id.name)
        }
      },

      TSParameterProperty(node) {
        // Track parameters with LocalDate type
        if (
          node.parameter?.type === 'Identifier' &&
          isLocalDateTypeAnnotation(
            node.parameter.typeAnnotation?.typeAnnotation
          )
        ) {
          localDateVariables.add(node.parameter.name)
        }
      },

      Identifier(node) {
        // Track variables with LocalDate type annotation
        if (isLocalDateTypeAnnotation(node.typeAnnotation?.typeAnnotation)) {
          localDateVariables.add(node.name)
        }
      },

      BinaryExpression(node) {
        if (!hasLocalDateImport || node.operator !== '===') {
          return
        }

        // Check if both operands are LocalDate instances
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
