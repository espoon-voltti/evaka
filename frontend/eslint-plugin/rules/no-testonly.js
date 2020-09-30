// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function isCallExpression(node) {
  return node.type === 'CallExpression'
}

function isMethodOf(name, node) {
  return node.callee.object && node.callee.object.name === name
}

function isCallOfOnly(node) {
  return node.callee.property && node.callee.property.name === 'only'
}

function isTestOnlyCall(node) {
  return isCallExpression(node) && isMethodOf('test', node) && isCallOfOnly(node)
}

function isFixtureOnlyCall(node) {
  return isCallExpression(node) && isMethodOf('fixture', node) && isCallOfOnly(node)
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'disallow test.only and fixture.only',
      category: 'Best Practices',
      recommended: true
    },
    schema: []
  },
  create: function (context) {
    return {
      CallExpression(node) {
        if (isTestOnlyCall(node)) {
          context.report({
            node,
            message: 'Use of test.only'
          })
        }

        if (isFixtureOnlyCall(node)) {
          context.report({
            node,
            message: 'Use of fixture.only'
          })
        }
      }
    }
  }
}
