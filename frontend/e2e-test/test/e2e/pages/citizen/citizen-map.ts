// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'
import { Daycare } from '../../dev-api/types'
import { SelectionChip } from '../../utils/helpers'

export default class CitizenMapPage {
  readonly mapView = Selector('[data-qa="map-view"]')

  readonly daycareFilter = Selector('[data-qa="map-filter-daycare"]')
  readonly preschoolFilter = Selector('[data-qa="map-filter-preschool"]')
  readonly clubFilter = Selector('[data-qa="map-filter-club"]')

  async setLanguageFilters(selected: { fi: boolean; sv: boolean }) {
    await this.setLanguageFilter('fi', selected.fi)
    await this.setLanguageFilter('sv', selected.sv)
  }
  async setLanguageFilter(language: 'fi' | 'sv', selected: boolean) {
    const chip = new SelectionChip(
      Selector(`[data-qa="map-filter-${language}"]`)
    )
    if ((await chip.selected) !== selected) {
      await chip.click()
    }
  }

  unitListItem(daycare: Daycare): Selector {
    return Selector(`[data-qa="map-unit-list-${daycare.id}"]`)
  }

  unitMapMarker(daycare: Daycare): Selector {
    return Selector(`[data-qa="map-marker-${daycare.id}"]`)
  }
}
