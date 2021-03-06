// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careAreaFixture,
  clubFixture,
  daycare2Fixture,
  Fixture,
  preschoolFixture
} from 'e2e-test-common/dev-api/fixtures'
import { Daycare } from 'e2e-test-common/dev-api/types'
import {
  DigitransitFeature,
  putDigitransitAutocomplete,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import CitizenMapPage from '../../pages/citizen/citizen-map'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../utils'

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
    locality: 'Espoo'
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
  page = await (await newBrowserContext()).newPage()
  await page.goto(config.enduserUrl)
  mapPage = new CitizenMapPage(page)
})
afterEach(async () => {
  await page.close()
})

describe('Citizen map page', () => {
  test('Unit type filter affects the unit list', async () => {
    await waitUntilTrue(() => mapPage.daycareFilter.visible)
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await waitUntilTrue(() => mapPage.listItemFor(daycare2Fixture).visible)
    await waitUntilFalse(() => mapPage.listItemFor(clubFixture).visible)
    await waitUntilTrue(() => mapPage.listItemFor(preschoolFixture).visible)

    await mapPage.clubFilter.click()
    await waitUntilTrue(() => mapPage.listItemFor(clubFixture).visible)
    await waitUntilFalse(() => mapPage.listItemFor(daycare2Fixture).visible)
    await waitUntilFalse(() => mapPage.listItemFor(preschoolFixture).visible)

    await mapPage.preschoolFilter.click()
    await waitUntilFalse(() => mapPage.listItemFor(clubFixture).visible)
    await waitUntilTrue(() => mapPage.listItemFor(preschoolFixture).visible)
    await waitUntilFalse(() => mapPage.listItemFor(daycare2Fixture).visible)
  })
  test('Unit language filter affects the unit list', async () => {
    await waitUntilTrue(() => mapPage.daycareFilter.visible)
    expect(await mapPage.daycareFilter.checked).toBe(true)
    await waitUntilTrue(() => mapPage.listItemFor(daycare2Fixture).visible)
    await waitUntilTrue(() => mapPage.listItemFor(swedishDaycare).visible)

    await mapPage.setLanguageFilter('fi', true)
    await waitUntilFalse(() => mapPage.listItemFor(swedishDaycare).visible)
    await waitUntilTrue(() => mapPage.listItemFor(daycare2Fixture).visible)

    await mapPage.setLanguageFilter('sv', true)
    await mapPage.setLanguageFilter('fi', false)
    await waitUntilTrue(() => mapPage.listItemFor(swedishDaycare).visible)
    await waitUntilFalse(() => mapPage.listItemFor(daycare2Fixture).visible)
  })
  test('Unit details can be viewed by clicking a list item', async () => {
    await mapPage.listItemFor(daycare2Fixture).click()
    await waitUntilTrue(() => mapPage.unitDetailsPanel.visible)
    await waitUntilEqual(
      () => mapPage.unitDetailsPanel.name,
      daycare2Fixture.name
    )

    await mapPage.unitDetailsPanel.backButton.click()
    await mapPage.listItemFor(swedishDaycare).click()

    await waitUntilTrue(() => mapPage.unitDetailsPanel.visible)
    await waitUntilEqual(
      () => mapPage.unitDetailsPanel.name,
      swedishDaycare.name
    )
  })
  test('Units can be searched', async () => {
    await mapPage.searchInput.type('Svart')
    await mapPage.searchInput.clickUnitResult(swedishDaycare)
    await waitUntilTrue(() => mapPage.unitDetailsPanel.visible)
    await waitUntilEqual(
      () => mapPage.unitDetailsPanel.name,
      swedishDaycare.name
    )
  })
  test('Streets can be searched', async () => {
    await putDigitransitAutocomplete({
      features: [testStreet]
    })
    await mapPage.searchInput.type('Testikatu')
    await mapPage.searchInput.clickAddressResult(testStreet.properties.name)
    await waitUntilTrue(() => mapPage.map.addressMarker.visible)
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
