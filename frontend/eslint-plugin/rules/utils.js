// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function isCallExpression(node) {
  return node.type === 'CallExpression'
}

function isMethodOf(names, node) {
  return node.callee.object && names.includes(node.callee.object.name)
}

function isCallOf(names, node) {
  return node.callee.property && names.includes(node.callee.property.name)
}

module.exports = { isCallExpression, isMethodOf, isCallOf }
