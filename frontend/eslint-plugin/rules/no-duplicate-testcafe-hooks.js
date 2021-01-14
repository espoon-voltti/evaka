// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function getDuplicateNodeFinder(context, name) {
  return () => {
    const node = context.getAncestors().find(node => node.property && node.property.name === name)
    return context.report({
      node,
      message: `Multiple "${name}" hooks not allowed in the same fixture`
    })
  }
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'disallow test.only and fixture.only',
      category: 'Possible Errors',
      recommended: true
    },
    schema: []
  },
  create: (context) => {
    return [
      "before",
      "beforeEach",
      "after",
      "afterEach"
    ].reduce((o, hook) => {
      return Object.assign(o, {
        [`MemberExpression[property.name='${hook}'] MemberExpression[property.name='${hook}'] Identifier[name='fixture']`]: getDuplicateNodeFinder(context, hook)
      })
    }, {});
  }
}
