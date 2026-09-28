// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export function hasSubMenuAttention({
  hasPersonalDetailsTasks,
  unreadDecisions
}: {
  hasPersonalDetailsTasks: boolean
  unreadDecisions: number
}): boolean {
  return hasPersonalDetailsTasks || unreadDecisions > 0
}
