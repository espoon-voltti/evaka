// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const { isCallExpression, isMethodOf, isCallOf } = require('./utils')

function isTestOnlyCall(node) {
  return (
    isCallExpression(node) &&
    isMethodOf(['test', 'it', 'describe', 'fixture'], node) &&
    isCallOf(['only'], node)
  )
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'disallow test.only and similar',
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
            message: 'Use of `.only`'
          })
        }
      }
    }
  }
}
