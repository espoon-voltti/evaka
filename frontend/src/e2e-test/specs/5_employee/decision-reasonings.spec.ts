// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DecisionReasoningsPage } from '../../pages/employee/decision-reasonings'
import EmployeeNav from '../../pages/employee/employee-nav'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

test.describe('Employee - Decision reasonings', () => {
  let page: Page
  let decisionReasoningsPage: DecisionReasoningsPage

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    const admin = await Fixture.employee().admin().save()
    page = evaka
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    await new EmployeeNav(page).openAndClickDropdownMenuItem(
      'decision-reasonings'
    )
    decisionReasoningsPage = new DecisionReasoningsPage(page)
  })

  test('Generic reasoning can be created, edited, activated and deleted', async () => {
    // Create a not-ready generic reasoning
    await decisionReasoningsPage.addGenericButton.click()
    await decisionReasoningsPage.genericValidFrom.fill('01.08.2026')
    await decisionReasoningsPage.genericTextFi.fill('Suomenkielinen perustelu')
    await decisionReasoningsPage.genericTextSv.fill('Svenskspråkig motivering')
    await decisionReasoningsPage.genericSaveAsNotReadyButton.click()

    // Verify the card appears with not-ready status
    const card = decisionReasoningsPage.genericCard(0)
    await expect(card.status).toHaveText('Ei käytössä')

    // Edit it
    await card.editButton.click()
    await decisionReasoningsPage.genericTextFi.fill(
      'Päivitetty suomenkielinen perustelu'
    )
    await decisionReasoningsPage.genericSaveAsNotReadyButton.click()

    // Activate it via confirmation dialog
    const cardAfterEdit = decisionReasoningsPage.genericCard(0)
    await cardAfterEdit.editButton.click()
    await decisionReasoningsPage.genericSaveAndActivateButton.click()
    await decisionReasoningsPage.confirmModal()
    await expect(decisionReasoningsPage.genericCard(0).status).toHaveText(
      'Käytössä'
    )

    // Create a second not-ready reasoning and delete it
    await decisionReasoningsPage.addGenericButton.click()
    await decisionReasoningsPage.genericValidFrom.fill('01.08.2027')
    await decisionReasoningsPage.genericTextFi.fill('Poistettava perustelu')
    await decisionReasoningsPage.genericTextSv.fill('Motivering att ta bort')
    await decisionReasoningsPage.genericSaveAsNotReadyButton.click()

    const secondCard = decisionReasoningsPage.genericCard(0)
    await secondCard.deleteButton.click()
    await decisionReasoningsPage.confirmModal()

    // Only the first (activated) card should remain
    await expect(decisionReasoningsPage.genericCards).toHaveCount(1)
  })

  test('Active generic reasoning can be removed', async () => {
    await decisionReasoningsPage.addGenericButton.click()
    await decisionReasoningsPage.genericValidFrom.fill('01.08.2026')
    await decisionReasoningsPage.genericTextFi.fill('Suomenkielinen perustelu')
    await decisionReasoningsPage.genericTextSv.fill('Svenskspråkig motivering')
    await decisionReasoningsPage.genericSaveAndActivateButton.click()
    await decisionReasoningsPage.confirmModal()

    const card = decisionReasoningsPage.genericCard(0)
    await expect(card.status).toHaveText('Käytössä')

    await card.removeButton.click()
    await decisionReasoningsPage.confirmModal()

    await expect(decisionReasoningsPage.genericCards).toHaveCount(0)
    await expect(decisionReasoningsPage.toggleOutdatedGeneric).toBeHidden()
  })

  test('Individual reasoning can be created and removed', async () => {
    const section = decisionReasoningsPage.individualFi

    // Create an individual reasoning
    await section.addButton.click()
    await section.titleInput.fill('Otsikko FI')
    await section.textInput.fill('Teksti FI')
    await section.saveAndActivateButton.click()
    await decisionReasoningsPage.confirmModal()

    // Verify the card appears with active status
    const card = section.card(0)
    await expect(card.status).toHaveText('Käytettävissä')

    // Remove it
    await card.removeButton.click()
    await decisionReasoningsPage.confirmModal()

    // Card should no longer be in the active list
    await expect(section.cards).toHaveCount(0)

    // It should appear in the removed section
    await section.toggleRemoved.click()
    await expect(section.cards).toHaveCount(1)
  })

  test('Finnish and Swedish individual reasonings are separate collections', async () => {
    const fi = decisionReasoningsPage.individualFi
    const sv = decisionReasoningsPage.individualSv

    await fi.addButton.click()
    await fi.titleInput.fill('Otsikko FI')
    await fi.textInput.fill('Teksti FI')
    await fi.saveAndActivateButton.click()
    await decisionReasoningsPage.confirmModal()

    await expect(fi.cards).toHaveCount(1)
    await expect(sv.cards).toHaveCount(0)

    await sv.addButton.click()
    await sv.titleInput.fill('Titel SV')
    await sv.textInput.fill('Text SV')
    await sv.saveAndActivateButton.click()
    await decisionReasoningsPage.confirmModal()

    await expect(fi.cards).toHaveCount(1)
    await expect(sv.cards).toHaveCount(1)
    await expect(fi.card(0)).toContainText('Otsikko FI')
    await expect(sv.card(0)).toContainText('Titel SV')
  })

  test('Tab switching shows separate data per collection', async () => {
    // Create a generic reasoning in the DAYCARE tab (default)
    await decisionReasoningsPage.addGenericButton.click()
    await decisionReasoningsPage.genericValidFrom.fill('01.08.2026')
    await decisionReasoningsPage.genericTextFi.fill('Varhaiskasvatus perustelu')
    await decisionReasoningsPage.genericTextSv.fill('Småbarnspedagogik')
    await decisionReasoningsPage.genericSaveAsNotReadyButton.click()
    await expect(decisionReasoningsPage.genericCards).toHaveCount(1)

    // Switch to preschool tab — should be empty
    await decisionReasoningsPage.preschoolTab.click()
    await expect(decisionReasoningsPage.genericCards).toHaveCount(0)

    // Switch to club tab — should also be empty
    await decisionReasoningsPage.clubTab.click()
    await expect(decisionReasoningsPage.genericCards).toHaveCount(0)

    // Switch back to daycare — reasoning should still be there
    await decisionReasoningsPage.daycareTab.click()
    await expect(decisionReasoningsPage.genericCards).toHaveCount(1)
  })
})
