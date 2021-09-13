// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function isNullOrEmpty(str?: string | null): boolean {
  return !str || str.trim().length == 0
}
