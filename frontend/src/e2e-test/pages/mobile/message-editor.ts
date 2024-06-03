// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Checkbox,
  Element,
  Page,
  TextInput,
  TreeDropdown
} from '../../utils/page'

export default class MobileMessageEditor extends Element {
  constructor(public page: Page) {
    super(page.findByDataQa('message-editor'))
  }

  senderName = this.findByDataQa('sender-name')
  recipients = new TreeDropdown(this.findByDataQa('recipients'))
  title = new TextInput(this.findByDataQa('input-title'))
  content = new TextInput(this.findByDataQa('input-content'))
  urgent = new Checkbox(this.findByDataQa('checkbox-urgent'))

  send = this.findByDataQa('send-message-btn')
  discard = this.findByDataQa('discard-message-btn')
  close = this.find('[aria-label="Sulje"]')
  manyRecipientsWarning = this.findByDataQa('many-recipients-warning')
  manyRecipientsConfirm = this.manyRecipientsWarning.findByDataQa('modal-okBtn')

  async fillMessage(message: {
    title: string
    content: string
    urgent?: boolean
  }) {
    await this.title.fill(message.title)
    await this.content.fill(message.content)
    if (message.urgent ?? false) {
      await this.urgent.check()
    } else {
      await this.urgent.uncheck()
    }
  }
}
