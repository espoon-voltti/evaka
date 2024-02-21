// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  careAreaFixture,
  clubFixture,
  daycare2Fixture,
  Fixture,
  preschoolFixture
} from '../../dev-api/fixtures'
import { Daycare } from '../../dev-api/types'
import {
  putDigitransitAutocomplete,
  resetDatabase
} from '../../generated/api-clients'
import { Feature as DigitransitFeature } from '../../generated/api-types'
import CitizenMapPage from '../../pages/citizen/citizen-map'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'

const swedishDaycare: Daycare = {
  ...daycare2Fixture,
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

const privateDaycareWithoutPeriods: Daycare = {
  ...daycare2Fixture,
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
  await resetDatabase()
  const careArea = await Fixture.careArea().with(careAreaFixture).save()
  await Fixture.daycare().with(clubFixture).careArea(careArea).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
  await Fixture.daycare().with(preschoolFixture).careArea(careArea).save()
  await Fixture.daycare().with(swedishDaycare).careArea(careArea).save()
  await Fixture.daycare()
    .with(privateDaycareWithoutPeriods)
    .careArea(careArea)
    .save()
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
    await mapPage.listItemFor(daycare2Fixture).waitUntilVisible()
    await mapPage.listItemFor(clubFixture).waitUntilHidden()
    await mapPage.listItemFor(preschoolFixture).waitUntilVisible()

    await mapPage.clubFilter.check()
    await mapPage.listItemFor(clubFixture).waitUntilVisible()
    await mapPage.listItemFor(daycare2Fixture).waitUntilHidden()
    await mapPage.listItemFor(preschoolFixture).waitUntilHidden()

    await mapPage.preschoolFilter.check()
    await mapPage.listItemFor(clubFixture).waitUntilHidden()
    await mapPage.listItemFor(preschoolFixture).waitUntilVisible()
    await mapPage.listItemFor(daycare2Fixture).waitUntilHidden()
  })
  test('Unit language filter affects the unit list', async () => {
    await mapPage.daycareFilter.waitUntilVisible()
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await mapPage.listItemFor(daycare2Fixture).waitUntilVisible()
    await mapPage.listItemFor(swedishDaycare).waitUntilVisible()

    await mapPage.setLanguageFilter('fi', true)
    await mapPage.listItemFor(swedishDaycare).waitUntilHidden()
    await mapPage.listItemFor(daycare2Fixture).waitUntilVisible()

    await mapPage.setLanguageFilter('sv', true)
    await mapPage.setLanguageFilter('fi', false)
    await mapPage.listItemFor(swedishDaycare).waitUntilVisible()
    await mapPage.listItemFor(daycare2Fixture).waitUntilHidden()
  })
  test('Unit details can be viewed by clicking a list item', async () => {
    await mapPage.listItemFor(daycare2Fixture).click()
    await mapPage.unitDetailsPanel.waitUntilVisible()
    await waitUntilEqual(
      () => mapPage.unitDetailsPanel.name,
      daycare2Fixture.name
    )

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
    await mapPage.testMapPopup(daycare2Fixture)
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
