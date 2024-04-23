// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PilotFeature } from 'lib-common/generated/api-types/shared'

import {
  CareAreaBuilder,
  DaycareBuilder,
  Fixture
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import MobileNav from '../../pages/mobile/mobile-nav'
import { SettingsPage } from '../../pages/mobile/settings-page'
import UnitListPage from '../../pages/mobile/unit-list-page'
import { pairMobileDevice, pairPersonalMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let area: CareAreaBuilder
let unit: DaycareBuilder

const enabledPilotFeatures: PilotFeature[] = [
  'MESSAGING',
  'MOBILE',
  'PUSH_NOTIFICATIONS'
]

beforeEach(async () => {
  await resetServiceState()
  area = await Fixture.careArea().save()
  unit = await Fixture.daycare()
    .with({
      enabledPilotFeatures
    })
    .careArea(area)
    .save()
  page = await Page.open()
})

describe('Settings page push permission section', () => {
  it('should show prompt/granted states correctly', async () => {
    await Fixture.daycareGroup().daycare(unit).save()

    const mobileSignupUrl = await pairMobileDevice(unit.data.id)
    await page.goto(mobileSignupUrl)
    const nav = new MobileNav(page)
    await nav.settings.click()
    const settingsPage = new SettingsPage(page)
    const settings = settingsPage.notificationSettings

    await settings.permissionState.assertTextEquals('Ei käytössä')
    await settings.enableButton.waitUntilVisible()

    // testing the actual permission popup is not currently possible with Playwright, but
    // we can add a permission override to the browser context
    await page.page.context().grantPermissions(['notifications'])
    await page.reload()

    await settings.permissionState.assertTextEquals('Käytössä')
  })
})

describe('Settings page category/group sections', () => {
  it('should show settings and allow editing them for a normal unit-level mobile device', async () => {
    const groups = [
      await Fixture.daycareGroup().daycare(unit).save(),
      await Fixture.daycareGroup().daycare(unit).save()
    ] as const
    const mobileSignupUrl = await pairMobileDevice(unit.data.id)
    await page.page.context().grantPermissions(['notifications'])
    await page.goto(mobileSignupUrl)
    const nav = new MobileNav(page)
    await nav.settings.click()
    const settingsPage = new SettingsPage(page)

    const settings = settingsPage.notificationSettings

    const categoryCheckbox = settings.category('RECEIVED_MESSAGE')
    const groupCheckboxes = [
      settings.group(groups[0].data.id),
      settings.group(groups[1].data.id)
    ] as const

    await categoryCheckbox.waitUntilChecked(false)
    await groupCheckboxes[0].waitUntilChecked(false)
    await groupCheckboxes[1].waitUntilChecked(false)

    await settings.editButton.click()
    await categoryCheckbox.check()
    await groupCheckboxes[1].check()

    await settings.saveButton.click()
    await categoryCheckbox.waitUntilChecked(true)
    await groupCheckboxes[0].waitUntilChecked(false)
    await groupCheckboxes[1].waitUntilChecked(true)
  })
  it('should show settings and allow editing them for a personal mobile device', async () => {
    // personal mobile devices may have multiple units with multiple groups, and the settings page
    // only shows groups for the currently selected units, but internally retains state for the other units
    const group = await Fixture.daycareGroup().daycare(unit).save()
    const otherUnit = await Fixture.daycare()
      .careArea(area)
      .with({ enabledPilotFeatures })
      .save()
    const otherGroup = await Fixture.daycareGroup().daycare(otherUnit).save()
    const employee = await Fixture.employeeUnitSupervisor(unit.data.id)
      .withDaycareAcl(otherUnit.data.id, 'UNIT_SUPERVISOR')
      .save()
    const mobileSignupUrl = await pairPersonalMobileDevice(employee.data.id)
    await page.page.context().grantPermissions(['notifications'])
    await page.goto(mobileSignupUrl)

    // we need to select the first unit before reaching the normal app navigation
    const unitListPage = new UnitListPage(page)
    await unitListPage.unit(unit.data.id).click()

    const nav = new MobileNav(page)
    await nav.settings.click()
    const settingsPage = new SettingsPage(page)
    const settings = settingsPage.notificationSettings
    const categoryCheckbox = settings.category('RECEIVED_MESSAGE')

    // save settings in the context of the first unit
    await settings.editButton.click()
    await categoryCheckbox.check()
    await settings.group(group.data.id).check()
    await settings.saveButton.click()

    // switch to the other unit
    await settingsPage.goBack.click()
    await unitListPage.unit(otherUnit.data.id).click()

    await nav.settings.click()
    // category is not unit-specific, so it should remember we already checked it
    await categoryCheckbox.waitUntilChecked(true)
    await settings.group(otherGroup.data.id).waitUntilChecked(false)

    // save settings in the context of the other unit
    await settings.editButton.click()
    await settings.group(otherGroup.data.id).check()
    await settings.saveButton.click()

    // switch to the first unit again
    await settingsPage.goBack.click()
    await unitListPage.unit(unit.data.id).click()

    await nav.settings.click()
    // both the category *and* the group selection should be retained even though
    // we saved the settings in the context of the other unit
    await categoryCheckbox.waitUntilChecked(true)
    await settings.group(group.data.id).waitUntilChecked(true)
  })
})
