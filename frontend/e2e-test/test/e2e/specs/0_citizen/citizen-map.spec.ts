// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import {
  careAreaFixture,
  clubFixture,
  daycare2Fixture,
  Fixture,
  preschoolFixture
} from '../../dev-api/fixtures'
import config from '../../config'
import CitizenMapPage from '../../pages/citizen/citizen-map'
import { Daycare } from '../../dev-api/types'

const mapPage = new CitizenMapPage()

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

fixture('Citizen map page')
  .meta({ type: 'regression', subType: 'citizen-map' })
  .before(async () => {
    const careArea = await Fixture.careArea().with(careAreaFixture).save()
    await Fixture.daycare().with(clubFixture).careArea(careArea).save()
    await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
    await Fixture.daycare().with(preschoolFixture).careArea(careArea).save()
    await Fixture.daycare().with(swedishDaycare).careArea(careArea).save()
  })
  .beforeEach(async (t) => {
    await t.navigateTo(config.enduserUrl)
    await t.click('[data-qa="nav-map"]')
    await t.expect(mapPage.mapView.visible).ok()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await Fixture.cleanup()
  })

test('Unit type filter affects the unit list', async (t) => {
  await t.expect(mapPage.daycareFilter.find('input').checked).ok()
  await t.expect(mapPage.unitListItem(daycare2Fixture).exists).ok()
  await t.expect(mapPage.unitListItem(clubFixture).exists).notOk()
  await t.expect(mapPage.unitListItem(preschoolFixture).exists).ok()

  await t.click(mapPage.clubFilter)
  await t.expect(mapPage.unitListItem(clubFixture).exists).ok()
  await t.expect(mapPage.unitListItem(daycare2Fixture).exists).notOk()
  await t.expect(mapPage.unitListItem(preschoolFixture).exists).notOk()

  await t.click(mapPage.preschoolFilter)
  await t.expect(mapPage.unitListItem(clubFixture).exists).notOk()
  await t.expect(mapPage.unitListItem(preschoolFixture).exists).ok()
  await t.expect(mapPage.unitListItem(daycare2Fixture).exists).notOk()
})

test('Unit language filter affects the unit list', async (t) => {
  await t.expect(mapPage.daycareFilter.find('input').checked).ok()

  await mapPage.setLanguageFilters({ fi: false, sv: false })
  await t.expect(mapPage.unitListItem(daycare2Fixture).exists).ok()
  await t.expect(mapPage.unitListItem(swedishDaycare).exists).ok()

  await mapPage.setLanguageFilter('fi', true)
  await t.debug()
  await t.expect(mapPage.unitListItem(swedishDaycare).exists).notOk()
  await t.expect(mapPage.unitListItem(daycare2Fixture).exists).ok()

  await mapPage.setLanguageFilters({ fi: false, sv: true })
  await t.expect(mapPage.unitListItem(swedishDaycare).exists).ok()
  await t.expect(mapPage.unitListItem(daycare2Fixture).exists).notOk()
})

test('Unit details can be viewed by clicking a list item', async (t) => {
  await t.click(mapPage.unitListItem(daycare2Fixture))
  await t.expect(mapPage.unitDetailsPanel.exists).ok()
  await t.expect(mapPage.unitDetailsPanel.name).eql(daycare2Fixture.name)

  await t.click(mapPage.unitDetailsPanel.backButton())
  await t.click(mapPage.unitListItem(swedishDaycare))

  await t.expect(mapPage.unitDetailsPanel.name).eql(swedishDaycare.name)
})

test('Viewing unit details automatically pans the map to the right marker', async (t) => {
  const daycare2Marker = mapPage.unitMapMarker(daycare2Fixture)
  const swedishMarker = mapPage.unitMapMarker(swedishDaycare)

  // Zoom in fully to make sure we start without either marker visible.
  await mapPage.map.zoomInFully()
  await t.expect(daycare2Marker.visible).ok()

  await t.click(mapPage.unitListItem(daycare2Fixture))
  await t.expect(daycare2Marker.visible).ok()

  await t.click(mapPage.unitDetailsPanel.backButton())
  await t.click(mapPage.unitListItem(swedishDaycare))
  await t.expect(swedishMarker.visible).ok()
})
