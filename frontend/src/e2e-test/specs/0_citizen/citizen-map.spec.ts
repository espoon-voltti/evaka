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

    await mapPage.setLanguageFilter('fi', true)
    await expect(mapPage.listItemFor(swedishDaycare)).toBeHidden()
    await expect(mapPage.listItemFor(testDaycare2)).toBeVisible()

    await mapPage.setLanguageFilter('sv', true)
    await mapPage.setLanguageFilter('fi', false)
    await expect(mapPage.listItemFor(swedishDaycare)).toBeVisible()
    await expect(mapPage.listItemFor(testDaycare2)).toBeHidden()
  })

  test('Unit details can be viewed by clicking a list item', async () => {
    await mapPage.listItemFor(testDaycare2).click()
    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      testDaycare2.name,
      { useInnerText: true }
    )

    await mapPage.unitDetailsPanel.backButton.click()
    await mapPage.listItemFor(swedishDaycare).click()

    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      swedishDaycare.name,
      { useInnerText: true }
    )
  })

  test('Units can be searched', async () => {
    await mapPage.searchInput.type('Svart')
    await mapPage.searchInput.clickUnitResult(swedishDaycare)
    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      swedishDaycare.name,
      { useInnerText: true }
    )
  })

  test('Units can be searched by middle words in the name', async () => {
    await mapPage.searchInput.type('svenska')
    await mapPage.searchInput.clickUnitResult(swedishDaycare)
    await expect(mapPage.unitDetailsPanel).toBeVisible()
    await expect(mapPage.unitDetailsPanel.nameElement).toHaveText(
      swedishDaycare.name,
      { useInnerText: true }
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
    ).toHaveText('Ei hakua eVakan kautta, ota yhteys yksikköön', {
      useInnerText: true
    })
  })
})
