// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export async function tap(dataQa: string) {
  const el = $(`[data-qa="${dataQa}"]`)
  await el.waitForDisplayed()
  await el.click()
}
