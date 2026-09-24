// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { TodoItemsPage } from '../../pages/employee/todo-items'
import { expect, test } from '../../playwright'
import { employeeLogin } from '../../utils/user'

test.describe('Employee - To-do items', () => {
  test.beforeEach(async () => {
    await resetServiceState()
  })

  test('adding and deleting to-do items', async ({ evaka }) => {
    const serviceWorker = await Fixture.employee().serviceWorker().save()
    await employeeLogin(evaka, serviceWorker)
    const todoItemsPage = await TodoItemsPage.open(evaka)

    await todoItemsPage.addNewButton.click()
    await todoItemsPage.descriptionInput.fill('Soita huoltajalle')
    await todoItemsPage.saveButton.click()
    await expect(todoItemsPage.itemDescriptions).toHaveText([
      'Soita huoltajalle'
    ])

    await todoItemsPage.addNewButton.click()
    await todoItemsPage.descriptionInput.fill('Tarkista hakemukset')
    await todoItemsPage.deadlineInput.fill(LocalDate.of(2026, 10, 1))
    await todoItemsPage.saveButton.click()
    await expect(todoItemsPage.itemDescriptions).toHaveText([
      'Tarkista hakemukset',
      'Soita huoltajalle'
    ])
    await expect(todoItemsPage.itemDeadlines).toHaveText(['01.10.2026', '–'])

    await todoItemsPage.items.first().findByDataQa('delete-button').click()
    await expect(todoItemsPage.itemDescriptions).toHaveText([
      'Soita huoltajalle'
    ])
  })
})
