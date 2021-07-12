// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { t, Selector } from 'testcafe'
import { selectFirstComboboxOption } from '../../../utils/helpers'

export default class MessagesPage {
  readonly unitsListUnit = (id: string) => Selector(`[data-qa="unit-${id}"]`)
  readonly draftBoxes = Selector(`[data-qa="message-box-row-DRAFTS"]`)
  readonly draftBox = (index: number) => this.draftBoxes.nth(index)
  readonly draftMessageRows = Selector(`[data-qa="draft-message-row"]`)
  readonly draftMessageRow = (index: number) => this.draftMessageRows.nth(index)
  readonly messageEditorTitle = Selector('[data-qa="input-title"]')
  readonly messageEditorContent = Selector('[data-qa="input-content"]')
  readonly messageEditorSender = Selector('[data-qa="select-sender"]')
  readonly messageEditorReceiver = Selector('[data-qa="select-receiver"]')
  readonly newMessageBtn = Selector('[data-qa="new-message-btn"]')
  readonly closeEditorBtn = Selector('[data-qa="close-message-editor-btn"]')
  readonly sendMessageBtn = Selector('[data-qa="send-message-btn"]')
  readonly discardDraftBtn = Selector('[data-qa="discard-draft-btn"]')
  private readonly groupSelector = Selector('[data-qa="group-selector"]')

  async draftNewMessage(title: string, content: string) {
    await t.click(this.newMessageBtn)
    await selectFirstComboboxOption(this.messageEditorSender)
    await t.click(this.messageEditorReceiver)
    await t.pressKey('enter')
    await t.typeText(this.messageEditorTitle, title)
    await t.typeText(this.messageEditorContent, content)
    // Draft is not saved immediately so we wait for a bit.
    await t.wait(500)
  }

  async sendNewMessage(title: string, content: string) {
    await t.click(this.newMessageBtn)
    await t.typeText(this.messageEditorTitle, title)
    await t.typeText(this.messageEditorContent, content)
    await selectFirstComboboxOption(this.messageEditorSender)
    await t.click(this.messageEditorReceiver)
    await t.pressKey('enter')
    await t.click(this.sendMessageBtn)
  }
}
