// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { Daycare } from 'e2e-test-common/dev-api/types'
import { delay, waitUntilEqual } from '../../utils'
import {
  descendantInput,
  RawElement,
  Radio,
  SelectionChip,
  WithTextInput
} from 'e2e-playwright/utils/element'

export default class CitizenMapPage {
  constructor(private readonly page: Page) {}

  readonly daycareFilter = new Radio(
    this.page,
    '[data-qa="map-filter-daycare"]'
  )
  readonly preschoolFilter = new Radio(
    this.page,
    '[data-qa="map-filter-preschool"]'
  )
  readonly clubFilter = new Radio(this.page, '[data-qa="map-filter-club"]')

  readonly unitDetailsPanel = new UnitDetailsPanel(
    this.page,
    '[data-qa="map-unit-details"]'
  )

  readonly map = new Map(this.page, '[data-qa="map-view"]')
  readonly searchInput = new MapSearchInput(
    this.page,
    '[data-qa="map-search-input"]'
  )
  readonly languageChips = {
    fi: new SelectionChip(this.page, '[data-qa="map-filter-fi"]'),
    sv: new SelectionChip(this.page, '[data-qa="map-filter-sv"]')
  }

  async setLanguageFilter(language: 'fi' | 'sv', selected: boolean) {
    const chip = this.languageChips[language]
    if ((await chip.checked) !== selected) {
      await chip.click()
    }
  }

  listItemFor(daycare: Daycare) {
    return new RawElement(this.page, `[data-qa="map-unit-list-${daycare.id}"]`)
  }

  async testMapPopup(daycare: Daycare) {
    await delay(500)
    await this.listItemFor(daycare).click()
    await delay(500)
    await this.map.markerFor(daycare).click()
    await waitUntilEqual(() => this.map.popupFor(daycare).name, daycare.name)
  }
}

class Map extends RawElement {
  static readonly MAX_ZOOM_ATTEMPTS = 30
  readonly #zoomIn = `${this.selector} .leaflet-control-zoom-in`
  readonly #zoomOut = `${this.selector} .leaflet-control-zoom-out`
  readonly #container = new RawElement(
    this.page,
    `${this.selector} .leaflet-container`
  )
  readonly addressMarker = new RawElement(
    this.page,
    `${this.selector} [data-qa="map-marker-address"]`
  )
  readonly markerCluster = new RawElement(this.page, '.marker-cluster')

  get zoomInDisabled(): Promise<boolean> {
    return this.page.$eval(this.#zoomIn, (el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  get zoomOutDisabled(): Promise<boolean> {
    return this.page.$eval(this.#zoomOut, (el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  async zoomOut(times: number | null = null) {
    let attempts = times ?? Map.MAX_ZOOM_ATTEMPTS
    while (attempts > 0) {
      if (await this.zoomOutDisabled) {
        return
      }
      await this.page.click(this.#zoomOut)
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
      await this.page.click(this.#zoomIn)
      attempts--
      if (attempts === 0) return
      await delay(100)
    }
    throw new Error(`Failed to zoom in after ${attempts} attempts`)
  }

  async zoomInFully() {
    return this.zoomIn()
  }

  markerFor(daycare: Daycare) {
    return new RawElement(this.page, `[title="${daycare.name}"]`)
  }
  popupFor(daycare: Daycare): MapPopup {
    return new MapPopup(this.page, `[data-qa="map-popup-${daycare.id}"]`)
  }

  async isMarkerInView(marker: MapMarker): Promise<boolean> {
    return (async () => {
      const [mapBox, markerBox] = await Promise.all([
        this.#container.boundingBox,
        marker.boundingBox
      ])
      return mapBox.contains(markerBox)
    })()
  }
}

class UnitDetailsPanel extends RawElement {
  readonly #name = `${this.selector} [data-qa="map-unit-details-name"]`
  readonly backButton = new RawElement(
    this.page,
    `${this.selector} [data-qa="map-unit-details-back"]`
  )

  get name(): Promise<string | null> {
    return this.page.textContent(this.#name)
  }
}

class MapMarker extends RawElement {}

class MapPopup extends RawElement {
  readonly #name = `${this.selector} [data-qa="map-popup-name"]`
  readonly #noApplying = `${this.selector} [data-qa="map-popup-no-applying"]`

  get name(): Promise<string | null> {
    return this.page.textContent(this.#name)
  }

  get noApplying(): Promise<string | null> {
    return this.page.textContent(this.#noApplying)
  }
}

class MapSearchInput extends WithTextInput(RawElement, descendantInput) {
  async clickUnitResult(daycare: Daycare) {
    await this.page.click(
      `${this.selector} [data-qa="map-search-${daycare.id}"]`
    )
  }

  async clickAddressResult(streetAddress: string) {
    await this.page.click(
      `${this.selector} [data-qa="map-search-address"][data-address="${streetAddress}"]`
    )
  }
}
