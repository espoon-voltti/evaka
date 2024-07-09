// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'fs'
import { promisify } from 'util'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import { DocumentTemplatesListPage } from '../../pages/employee/documents/document-templates'
import EmployeeNav from '../../pages/employee/employee-nav'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: DevEmployee
let page: Page

beforeEach(async () => {
  await resetServiceState()

  admin = await Fixture.employeeAdmin().save()
  page = await Page.open()
  await employeeLogin(page, admin)
  await page.goto(config.employeeUrl)
})

describe('Employee - Document templates', () => {
  test('A document template can be exported and imported', async () => {
    const template = await Fixture.documentTemplate()
      .with({
        content: {
          sections: [
            {
              id: 's1',
              label: 'osio 1',
              infoText: '',
              questions: []
            }
          ]
        }
      })
      .save()
    const nav = new EmployeeNav(page)
    await nav.openAndClickDropdownMenuItem('document-templates')
    const templates = new DocumentTemplatesListPage(page)

    const jsonPath = await templates.templateRow(template.name).exportToPath()
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    const data = JSON.parse(
      await promisify(fs.readFile)(jsonPath, {
        encoding: 'utf-8'
      })
    )
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    expect(data.name).toEqual(template.name)

    const name = 'Tuodun uusi nimi'

    const importModal = await templates.openImportModal()
    await importModal.file.setInputFiles(jsonPath)
    await importModal.continue.click()
    const modal = templates.templateModal
    await modal.nameInput.fill(name)
    await modal.confirmCreateButton.click()

    await templates.templateRow(name).waitUntilVisible()
  })
})
