// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevDaycare } from '../../generated/api-types'
import { delay, waitUntilEqual } from '../../utils'
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
  constructor(private readonly page: Page) {
    this.daycareFilter = new Radio(page.findByDataQa('map-filter-DAYCARE'))
    this.preschoolFilter = new Radio(page.findByDataQa('map-filter-PRESCHOOL'))
    this.clubFilter = new Radio(page.findByDataQa('map-filter-CLUB'))
    this.unitDetailsPanel = new UnitDetailsPanel(
      page.findByDataQa('map-unit-details')
    )
    this.map = new Map(page.findByDataQa('map-view'))
    this.searchInput = new MapSearchInput(page.findByDataQa('map-search-input'))
  }

  readonly languageChips = {
    fi: new SelectionChip(this.page.findByDataQa('map-filter-fi')),
    sv: new SelectionChip(this.page.findByDataQa('map-filter-sv'))
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
  readonly #zoomIn = this.find('.leaflet-control-zoom-in')
  readonly #zoomOut = this.find('.leaflet-control-zoom-out')

  readonly addressMarker = this.find('[data-qa="map-marker-address"]')

  get zoomInDisabled(): Promise<boolean> {
    return this.#zoomIn.evaluate((el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  get zoomOutDisabled(): Promise<boolean> {
    return this.#zoomOut.evaluate((el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  async zoomOut(times: number | null = null) {
    let attempts = times ?? Map.MAX_ZOOM_ATTEMPTS
    while (attempts > 0) {
      if (await this.zoomOutDisabled) {
        return
      }
      await this.#zoomOut.click()
      attempts--
      if (attempts === 0) return
      await delay(100)
    }
    throw new Error(`Failed to zoom out after ${attempts} attempts`)
  }

  async zoomIn(times: number | null = null) {
    let attempts = times ?? Map.MAX_ZOOM_ATTEMPTS
    while (attempts > 0) {
      if (await this.zoomInDisabled) {
        return
      }
      await this.#zoomIn.click()
      attempts--
      if (attempts === 0) return
      await delay(100)
    }
    throw new Error(`Failed to zoom in after ${attempts} attempts`)
  }

  async zoomInFully() {
    return this.zoomIn()
  }

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
