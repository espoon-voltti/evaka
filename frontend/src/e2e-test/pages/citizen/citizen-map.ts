// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevDaycare } from '../../generated/api-types'
import { waitUntilEqual } from '../../utils'
import {
  Element,
  Page,
  Radio,
  SelectionChip,
  TextInput
} from '../../utils/page'

export default class CitizenMapPage {
  daycareFilter: Radio
  preschoolFilter: Radio
  clubFilter: Radio
  unitDetailsPanel: UnitDetailsPanel
  map: Map
  searchInput: MapSearchInput
  languageChips: { fi: SelectionChip; sv: SelectionChip }
  constructor(private readonly page: Page) {
    this.daycareFilter = new Radio(page.findByDataQa('map-filter-DAYCARE'))
    this.preschoolFilter = new Radio(page.findByDataQa('map-filter-PRESCHOOL'))
    this.clubFilter = new Radio(page.findByDataQa('map-filter-CLUB'))
    this.unitDetailsPanel = new UnitDetailsPanel(
      page.findByDataQa('map-unit-details')
    )
    this.map = new Map(page.findByDataQa('map-view'))
    this.searchInput = new MapSearchInput(page.findByDataQa('map-search-input'))
    this.languageChips = {
      fi: new SelectionChip(page.findByDataQa('map-filter-fi')),
      sv: new SelectionChip(page.findByDataQa('map-filter-sv'))
    }
  }

  async setLanguageFilter(language: 'fi' | 'sv', selected: boolean) {
    const chip = this.languageChips[language]
    if ((await chip.checked) !== selected) {
      await chip.click()
    }
  }

  listItemFor(daycare: DevDaycare) {
    return this.page.findByDataQa(`map-unit-list-${daycare.id}`)
  }

  async testMapPopup(daycare: DevDaycare) {
    await this.listItemFor(daycare).click()
    await this.map.markerFor(daycare).click()
    await waitUntilEqual(() => this.map.popupFor(daycare).name, daycare.name)
  }
}

class Map extends Element {
  static readonly MAX_ZOOM_ATTEMPTS = 30

  readonly addressMarker = this.find('[data-qa="map-marker-address"]')

  markerFor(daycare: DevDaycare) {
    return this.find(`[title="${daycare.name}"]`)
  }

  popupFor(daycare: DevDaycare): MapPopup {
    return new MapPopup(this.find(`[data-qa="map-popup-${daycare.id}"]`))
  }
}

class UnitDetailsPanel extends Element {
  readonly backButton = this.find('[data-qa="map-unit-details-back"]')

  get name(): Promise<string | null> {
    return this.find('[data-qa="map-unit-details-name"]').text
  }
}

class MapPopup extends Element {
  readonly #name = this.find('[data-qa="map-popup-name"]')
  readonly #noApplying = this.find('[data-qa="map-popup-no-applying"]')

  get name(): Promise<string | null> {
    return this.#name.text
  }

  get noApplying(): Promise<string | null> {
    return this.#noApplying.text
  }
}

class MapSearchInput extends Element {
  async type(text: string) {
    await new TextInput(this.find('input')).type(text)
  }

  async clickUnitResult(daycare: DevDaycare) {
    await this.find(`[data-qa="map-search-${daycare.id}"]`).click()
  }

  async clickAddressResult(streetAddress: string) {
    await this.find(
      `[data-qa="map-search-address"][data-address="${streetAddress}"]`
    ).click()
  }
}
