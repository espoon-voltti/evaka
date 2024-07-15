// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
import {
  DevDaycare,
  Feature as DigitransitFeature
} from '../../generated/api-types'
import CitizenMapPage from '../../pages/citizen/citizen-map'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'

const swedishDaycare: DevDaycare = {
  ...testDaycare2,
  name: 'Svart hål svenska daghem',
  id: '9db9e8f7-2091-4be1-b091-fe107906e1b9',
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
  id: '9db9e8f7-2091-4be1-b091-fe10790e107a',
  location: { lat: 60.1601417, lon: 24.7830233 },
  daycareApplyPeriod: null,
  preschoolApplyPeriod: null,
  clubApplyPeriod: null,
  providerType: 'PRIVATE'
}

let page: Page
let mapPage: CitizenMapPage
beforeAll(async () => {
  await resetServiceState()
  const careArea = await Fixture.careArea(testCareArea).save()
  await Fixture.daycare({ ...testClub, areaId: careArea.id }).save()
  await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
  await Fixture.daycare({ ...testPreschool, areaId: careArea.id }).save()
  await Fixture.daycare({ ...swedishDaycare, areaId: careArea.id }).save()
  await Fixture.daycare({
    ...privateDaycareWithoutPeriods,
    areaId: careArea.id
  }).save()
})
beforeEach(async () => {
  page = await Page.open()
  await page.goto(`${config.enduserUrl}/map`)
  mapPage = new CitizenMapPage(page)
})

describe('Citizen map page', () => {
  test('Unit type filter affects the unit list', async () => {
    await mapPage.daycareFilter.waitUntilVisible()
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await mapPage.listItemFor(testDaycare2).waitUntilVisible()
    await mapPage.listItemFor(testClub).waitUntilHidden()
    await mapPage.listItemFor(testPreschool).waitUntilVisible()

    await mapPage.clubFilter.check()
    await mapPage.listItemFor(testClub).waitUntilVisible()
    await mapPage.listItemFor(testDaycare2).waitUntilHidden()
    await mapPage.listItemFor(testPreschool).waitUntilHidden()

    await mapPage.preschoolFilter.check()
    await mapPage.listItemFor(testClub).waitUntilHidden()
    await mapPage.listItemFor(testPreschool).waitUntilVisible()
    await mapPage.listItemFor(testDaycare2).waitUntilHidden()
  })
  test('Unit language filter affects the unit list', async () => {
    await mapPage.daycareFilter.waitUntilVisible()
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await mapPage.listItemFor(testDaycare2).waitUntilVisible()
    await mapPage.listItemFor(swedishDaycare).waitUntilVisible()

    await mapPage.setLanguageFilter('fi', true)
    await mapPage.listItemFor(swedishDaycare).waitUntilHidden()
    await mapPage.listItemFor(testDaycare2).waitUntilVisible()

    await mapPage.setLanguageFilter('sv', true)
    await mapPage.setLanguageFilter('fi', false)
    await mapPage.listItemFor(swedishDaycare).waitUntilVisible()
    await mapPage.listItemFor(testDaycare2).waitUntilHidden()
  })
  test('Unit details can be viewed by clicking a list item', async () => {
    await mapPage.listItemFor(testDaycare2).click()
    await mapPage.unitDetailsPanel.waitUntilVisible()
    await waitUntilEqual(() => mapPage.unitDetailsPanel.name, testDaycare2.name)

    await mapPage.unitDetailsPanel.backButton.click()
    await mapPage.listItemFor(swedishDaycare).click()

    await mapPage.unitDetailsPanel.waitUntilVisible()
    await waitUntilEqual(
      () => mapPage.unitDetailsPanel.name,
      swedishDaycare.name
    )
  })
  test('Units can be searched', async () => {
    await mapPage.searchInput.type('Svart')
    await mapPage.searchInput.clickUnitResult(swedishDaycare)
    await mapPage.unitDetailsPanel.waitUntilVisible()
    await waitUntilEqual(
      () => mapPage.unitDetailsPanel.name,
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
    await mapPage.map.addressMarker.waitUntilVisible()
  })
  test('Unit markers can be clicked to open a popup', async () => {
    await mapPage.testMapPopup(testDaycare2)
    await mapPage.unitDetailsPanel.backButton.click()
    await mapPage.testMapPopup(swedishDaycare)
  })
  test('Private unit without any periods will show up on the map', async () => {
    await mapPage.testMapPopup(privateDaycareWithoutPeriods)
    await waitUntilEqual(
      () => mapPage.map.popupFor(privateDaycareWithoutPeriods).noApplying,
      'Ei hakua eVakan kautta, ota yhteys yksikköön'
    )
  })
})
