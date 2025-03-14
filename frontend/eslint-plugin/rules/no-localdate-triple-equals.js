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

    const LOCALDATE_STATIC_METHODS = [
      'of',
      'parseIso',
      'parseFiOrThrow',
      'parseFiOrNull',
      'parseNullableIso',
      'tryParseIso',
      'tryCreate',
      'fromSystemTzDate',
      'todayInSystemTz',
      'todayInHelsinkiTz'
    ]

    const LOCALDATE_INSTANCE_METHODS = [
      'withDate',
      'addDays',
      'subDays',
      'startOfMonth',
      'lastDayOfMonth'
    ]

    const isLocalDateFactoryCall = (node) => {
      if (!node) return false

      // Check for static method calls (LocalDate.of(), etc.)
      if (
        node.type === 'CallExpression' &&
        node.callee.type === 'MemberExpression' &&
        node.callee.object.name === localDateIdentifier &&
        LOCALDATE_STATIC_METHODS.includes(node.callee.property.name)
      ) {
        return true
      }

      // Check for instance method calls (date.addDays(), etc.)
      if (
        node.type === 'CallExpression' &&
        node.callee.type === 'MemberExpression' &&
        LOCALDATE_INSTANCE_METHODS.includes(node.callee.property.name)
      ) {
        // Check if the object is a LocalDate instance
        return (
          node.callee.object.type === 'Identifier' &&
          localDateVariables.has(node.callee.object.name)
        )
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
        if (isLocalDateFactoryCall(node.init)) {
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
          return isLocalDateFactoryCall(expr)
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
