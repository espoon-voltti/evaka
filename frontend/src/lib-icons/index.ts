// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/triple-slash-reference
/// <reference path="./fontawesome.d.ts" />

export * from 'Icons'

// eslint-disable-next-line @typescript-eslint/consistent-type-imports
export type IconSet = typeof import('Icons')

export function typeAssert<T extends boolean>(_: T) {
  // empty
}
