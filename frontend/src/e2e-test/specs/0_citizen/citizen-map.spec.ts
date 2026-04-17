// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fromUuid } from 'lib-common/id-type'

import config from '../../config'
import {
  testCareArea,
  testClub,
  testDaycare2,
  Fixture,
  testPreschool
} from '../../dev-api/fixtures'
import {
  putDigitransitAutocomplete,
  resetServiceState
} from '../../generated/api-clients'
import type {
  DevDaycare,
  Feature as DigitransitFeature
} from '../../generated/api-types'
import CitizenMapPage from '../../pages/citizen/citizen-map'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'

const swedishDaycare: DevDaycare = {
  ...testDaycare2,
  name: 'Svart hål svenska daghem',
  id: fromUuid('9db9e8f7-2091-4be1-b091-fe107906e1b9'),
  language: 'sv',
  location: {
    lat: 60.200745762705296,
    lon: 24.785286409387005
  }
}

const englishDaycare: DevDaycare = {
  ...testDaycare2,
  name: 'Black hole english daycare',
  id: fromUuid('1d4ca4f9-e3d2-4c0c-9b56-9e72b41f8c2a'),
  language: 'en',
  location: {
    lat: 60.200745762705296,
    lon: 24.785286409387005
  }
}

const swedishPreschool: DevDaycare = {
  ...testPreschool,
  name: 'Svart hål svenska förskola',
  id: fromUuid('6c8e1f4b-3a52-4d7a-a8d6-5b1c0e2f9a73'),
  language: 'sv'
}

const testStreet: DigitransitFeature = {
  geometry: {
    coordinates: [24.700883345430185, 60.18686533339131]
  },
  properties: {
    name: 'Testikatu 42',
    postalcode: '00000',
    locality: 'Espoo',
    localadmin: null
  }
}

const privateDaycareWithoutPeriods: DevDaycare = {
  ...testDaycare2,
  name: 'Private daycare',
  id: fromUuid('9db9e8f7-2091-4be1-b091-fe10790e107a'),
  location: { lat: 60.1601417, lon: 24.7830233 },
  daycareApplyPeriod: null,
  preschoolApplyPeriod: null,
  clubApplyPeriod: null,
  providerType: 'PRIVATE'
}

test.describe('Citizen map page', () => {
  let page: Page
  let mapPage: CitizenMapPage

  test.beforeAll(async () => {
    await resetServiceState()
    const careArea = await testCareArea.save()
    await Fixture.daycare({ ...testClub, areaId: careArea.id }).save()
    await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
    await Fixture.daycare({ ...testPreschool, areaId: careArea.id }).save()
    await Fixture.daycare({ ...swedishDaycare, areaId: careArea.id }).save()
    await Fixture.daycare({ ...englishDaycare, areaId: careArea.id }).save()
    await Fixture.daycare({ ...swedishPreschool, areaId: careArea.id }).save()
    await Fixture.daycare({
      ...privateDaycareWithoutPeriods,
      areaId: careArea.id
    }).save()
  })

  test.beforeEach(async ({ evaka }) => {
    page = evaka
    await page.goto(`${config.enduserUrl}/map`)
    mapPage = new CitizenMapPage(page)
  })

  test('Unit type filter affects the unit list', async () => {
    await expect(mapPage.daycareFilter).toBeVisible()
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await expect(mapPage.listItemFor(testDaycare2)).toBeVisible()
    await expect(mapPage.listItemFor(testClub)).toBeHidden()
    await expect(mapPage.listItemFor(testPreschool)).toBeVisible()

    await mapPage.clubFilter.check()
    await expect(mapPage.listItemFor(testClub)).toBeVisible()
    await expect(mapPage.listItemFor(testDaycare2)).toBeHidden()
    await expect(mapPage.listItemFor(testPreschool)).toBeHidden()

    await mapPage.preschoolFilter.check()
    await expect(mapPage.listItemFor(testClub)).toBeHidden()
    await expect(mapPage.listItemFor(testPreschool)).toBeVisible()
    await expect(mapPage.listItemFor(testDaycare2)).toBeHidden()
  })

  test('Unit language filter affects the unit list', async () => {
    await expect(mapPage.daycareFilter).toBeVisible()
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await expect(mapPage.listItemFor(testDaycare2)).toBeVisible()
    await expect(mapPage.listItemFor(swedishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(englishDaycare)).toBeVisible()

    await mapPage.setLanguageFilter('fi', true)
    await expect(mapPage.listItemFor(swedishDaycare)).toBeHidden()
    await expect(mapPage.listItemFor(englishDaycare)).toBeHidden()
    await expect(mapPage.listItemFor(testDaycare2)).toBeVisible()

    await mapPage.setLanguageFilter('en', true)
    await expect(mapPage.listItemFor(swedishDaycare)).toBeHidden()
    await expect(mapPage.listItemFor(englishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(testDaycare2)).toBeVisible()

    await mapPage.setLanguageFilter('sv', true)
    await mapPage.setLanguageFilter('fi', false)
    await mapPage.setLanguageFilter('en', false)
    await expect(mapPage.listItemFor(swedishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(englishDaycare)).toBeHidden()
    await expect(mapPage.listItemFor(testDaycare2)).toBeHidden()

    await mapPage.setLanguageFilter('sv', false)
    await expect(mapPage.listItemFor(testDaycare2)).toBeVisible()
    await expect(mapPage.listItemFor(swedishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(englishDaycare)).toBeVisible()
  })

  test('Language filter is hidden when only one language exists in the current care type', async () => {
    // The shared seed has only the Finnish testClub for CLUB care type
    await mapPage.clubFilter.check()
    await mapPage.assertLanguageFilterVisible(false)

    // Switching back to DAYCARE (which has fi + sv + en units) shows the filter again
    await mapPage.daycareFilter.check()
    await mapPage.assertLanguageFilterVisible(true)
  })

  test('Language filter chips reflect available languages in the current care type', async () => {
    // DAYCARE has fi (testDaycare2) + sv (swedishDaycare) + en (englishDaycare)
    await expect(mapPage.languageChips.fi).toBeVisible()
    await expect(mapPage.languageChips.sv).toBeVisible()
    await expect(mapPage.languageChips.en).toBeVisible()

    // PRESCHOOL has fi (testPreschool) + sv (swedishPreschool) but no English unit
    await mapPage.preschoolFilter.check()
    await expect(mapPage.languageChips.fi).toBeVisible()
    await expect(mapPage.languageChips.sv).toBeVisible()
    await expect(mapPage.languageChips.en).toBeHidden()

    // Switching back to DAYCARE re-introduces the en chip
    await mapPage.daycareFilter.check()
    await expect(mapPage.languageChips.en).toBeVisible()
  })

  test('Language selections for unavailable languages are ignored on care type switch', async () => {
    // Select en in DAYCARE — only the English unit should remain
    await mapPage.setLanguageFilter('en', true)
    await expect(mapPage.listItemFor(englishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(testDaycare2)).toBeHidden()
    await expect(mapPage.listItemFor(swedishDaycare)).toBeHidden()

    // Switch to PRESCHOOL — no English preschools exist, so the stale en
    // selection is ignored and all preschool units are shown.
    await mapPage.preschoolFilter.check()
    await expect(mapPage.listItemFor(testPreschool)).toBeVisible()
    await expect(mapPage.listItemFor(swedishPreschool)).toBeVisible()

    // Adding an applicable chip (sv) filters normally; the stale en is still
    // ignored, so only the Swedish preschool matches
    await mapPage.setLanguageFilter('sv', true)
    await expect(mapPage.listItemFor(swedishPreschool)).toBeVisible()
    await expect(mapPage.listItemFor(testPreschool)).toBeHidden()

    // Switching back to DAYCARE reactivates both selections — en and sv
    // are both applicable there, so English and Swedish daycares show
    await mapPage.daycareFilter.check()
    await expect(mapPage.listItemFor(englishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(swedishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(testDaycare2)).toBeHidden()
  })

  test('Unit details can be viewed by clicking a list item', async () => {
    await mapPage.listItemFor(testDaycare2).click()
    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      testDaycare2.name
    )

    await mapPage.unitDetailsPanel.backButton.click()
    await mapPage.listItemFor(swedishDaycare).click()

    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      swedishDaycare.name
    )
  })

  test('Units can be searched', async () => {
    await mapPage.searchInput.type('Svart')
    await mapPage.searchInput.clickUnitResult(swedishDaycare)
    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      swedishDaycare.name
    )
  })

  test('Units can be searched by middle words in the name', async () => {
    await mapPage.searchInput.type('svenska')
    await mapPage.searchInput.clickUnitResult(swedishDaycare)
    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      swedishDaycare.name
    )
  })

  test('Streets can be searched', async () => {
    await putDigitransitAutocomplete({
      body: {
        features: [testStreet]
      }
    })
    await mapPage.searchInput.type('Testikatu')
    await mapPage.searchInput.clickAddressResult(testStreet.properties.name)
    await expect(mapPage.map.addressMarker).toBeVisible()
  })

  test('Unit markers can be clicked to open a popup', async () => {
    await mapPage.testMapPopup(testDaycare2)
    await mapPage.unitDetailsPanel.backButton.click()
    await mapPage.testMapPopup(swedishDaycare)
  })

  test('Private unit without any periods will show up on the map', async () => {
    await mapPage.testMapPopup(privateDaycareWithoutPeriods)
    await expect(
      mapPage.map.popupFor(privateDaycareWithoutPeriods).noApplying
    ).toHaveText('Ei hakua eVakan kautta, ota yhteys yksikköön')
  })
})
