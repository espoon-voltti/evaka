// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const none: unique symbol = Symbol('none')

export function memoizeLast<T>(fn: () => T): () => T
export function memoizeLast<S, T>(fn: (input: S) => T): (input: S) => T
export function memoizeLast<S, T>(fn: (input: S) => T): (input: S) => T {
  let lastInput: S | typeof none = none
  let lastOutput: T | typeof none = none
  return (input: S) => {
    if (input !== lastInput) {
      lastInput = input
      lastOutput = fn(input)
    }
    return lastOutput as T
  }
}
