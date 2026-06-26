// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Returns a promise that is resolved by a node-style callback function
export function fromCallback<T>(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  f: (cb: (err: any, result?: T) => void) => void
): Promise<T> {
  return new Promise<T>((resolve, reject) =>
    f((err, result) => (err ? reject(err) : resolve(result!)))
  )
}
