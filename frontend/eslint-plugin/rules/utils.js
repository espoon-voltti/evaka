// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function isCallExpression(node) {
  return node.type === 'CallExpression'
}

export function isMethodOf(names, node) {
  return node.callee.object && names.includes(node.callee.object.name)
}

export function isCallOf(names, node) {
  return node.callee.property && names.includes(node.callee.property.name)
}
