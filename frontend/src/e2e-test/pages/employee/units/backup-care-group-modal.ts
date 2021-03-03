// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export default class BackupCareGroupModal {
  readonly root = Selector('[data-qa="backup-care-group-modal"]')
  readonly submitButton = this.root.find('[data-qa="modal-okBtn"]')

  async submit() {
    await t.expect(this.submitButton.visible).ok()
    await t.click(this.submitButton)
  }
}
