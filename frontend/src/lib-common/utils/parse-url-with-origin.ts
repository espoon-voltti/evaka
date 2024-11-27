// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * Parses a string as a URL, requiring it to be either a relative URL, or to have exactly the correct origin.
 *
 * If the string does not pass the validation, undefined is returned.
 */
export function parseUrlWithOrigin(
  base: { origin: string },
  value: string
): URL | undefined {
  try {
    const url = new URL(value, base.origin)
    return url.origin === base.origin ? url : undefined
  } catch (err) {
    return undefined
  }
}
