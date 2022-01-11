// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const { isCallExpression, isMethodOf, isCallOf } = require('./utils')

function isPagePauseCall(node) {
  return (
    isCallExpression(node) &&
    isMethodOf(['page'], node) &&
    isCallOf(['pause'], node)
  )
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'disallow page.pause()',
      category: 'Best Practices',
      recommended: true
    },
    schema: []
  },
  create: function (context) {
    return {
      CallExpression(node) {
        if (isPagePauseCall(node)) {
          context.report({
            node,
            message: 'Use of page.pause()'
          })
        }
      }
    }
  }
}
