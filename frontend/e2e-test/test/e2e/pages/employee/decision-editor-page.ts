// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export default class DecisionEditorPage {
  async save() {
    const button: Selector = Selector('[data-qa="save-decisions-button"')
    await t.click(button)
  }
  async cancel() {
    const button: Selector = Selector('[data-qa="cancel-decisions-button"')
    await t.click(button)
  }
}
