// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import type { Element, ElementCollection, Page } from '../../utils/page'
import { AsyncButton, DatePicker, TextInput } from '../../utils/page'

export class TodoItemsPage {
  readonly addNewButton: Element
  readonly descriptionInput: TextInput
  readonly deadlineInput: DatePicker
  readonly saveButton: AsyncButton
  readonly items: ElementCollection
  readonly itemDescriptions: ElementCollection
  readonly itemDeadlines: ElementCollection

  constructor(page: Page) {
    this.addNewButton = page.findByDataQa('add-new-button')
    this.descriptionInput = new TextInput(
      page.findByDataQa('description-input')
    )
    this.deadlineInput = new DatePicker(page.findByDataQa('deadline-input'))
    this.saveButton = new AsyncButton(page.findByDataQa('save-button'))
    this.items = page.findAllByDataQa('todo-item')
    this.itemDescriptions = page.findAllByDataQa('todo-item-description')
    this.itemDeadlines = page.findAllByDataQa('todo-item-deadline')
  }

  static async open(page: Page) {
    await page.goto(config.employeeUrl + '/todo-items')
    return new TodoItemsPage(page)
  }
}
