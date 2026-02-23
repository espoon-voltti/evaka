// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PilotFeature } from 'lib-common/generated/api-types/shared'

import { mobileViewport } from '../../browser'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevCareArea, DevDaycare } from '../../generated/api-types'
import MobileNav from '../../pages/mobile/mobile-nav'
import { SettingsPage } from '../../pages/mobile/settings-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let area: DevCareArea
let unit: DevDaycare

const enabledPilotFeatures: PilotFeature[] = ['MESSAGING', 'MOBILE']

beforeEach(async () => {
  await resetServiceState()
  area = await Fixture.careArea().save()
  unit = await Fixture.daycare({
    enabledPilotFeatures,
    areaId: area.id
  }).save()
  page = await Page.open({ viewport: mobileViewport })
})

describe('Language selection', () => {
  it('should allow switching language and persist the selection', async () => {
    await Fixture.daycareGroup({ daycareId: unit.id }).save()

    const mobileSignupUrl = await pairMobileDevice(unit.id)
    await page.goto(mobileSignupUrl)
    const nav = new MobileNav(page)
    await nav.settings.click()
    const settingsPage = new SettingsPage(page)
    const langSelection = settingsPage.languageSelection

    // Default language is Finnish
    await settingsPage.title.assertTextEquals('Asetukset')
    await langSelection.fi.assertAttributeEquals('aria-checked', 'true')
    await langSelection.sv.assertAttributeEquals('aria-checked', 'false')

    // Switch to Swedish
    await langSelection.sv.click()
    await settingsPage.title.assertTextEquals('Inställningar')
    await langSelection.sv.assertAttributeEquals('aria-checked', 'true')
    await langSelection.fi.assertAttributeEquals('aria-checked', 'false')

    // Language persists after reload
    await page.reload()
    await settingsPage.title.assertTextEquals('Inställningar')
    await langSelection.sv.assertAttributeEquals('aria-checked', 'true')
  })
})
