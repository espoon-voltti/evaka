// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator, Page } from 'playwright'
import { Daycare } from 'e2e-test-common/dev-api/types'
import { delay, waitUntilEqual } from '../../utils'
import { CheckboxLocator } from '../../utils/element'

export default class CitizenMapPage {
  constructor(private readonly page: Page) {}

  readonly #languageChips = {
    fi: new CheckboxLocator(this.page.locator('[data-qa="map-filter-fi"]')),
    sv: new CheckboxLocator(this.page.locator('[data-qa="map-filter-sv"]'))
  }

  readonly map = new Map(this.page.locator('[data-qa="map-view"]'))

  readonly daycareFilter = new CheckboxLocator(
    this.page.locator('[data-qa="map-filter-daycare"]')
  )
  readonly preschoolFilter = this.page.locator(
    '[data-qa="map-filter-preschool"]'
  )
  readonly clubFilter = this.page.locator('[data-qa="map-filter-club"]')

  readonly unitDetailsPanel = new UnitDetailsPanel(
    this.page.locator('[data-qa="map-unit-details"]')
  )

  readonly searchInput = new MapSearchInput(
    this.page.locator('[data-qa="map-search-input"]')
  )

  async setLanguageFilter(language: 'fi' | 'sv', selected: boolean) {
    const chip = this.#languageChips[language]
    if ((await chip.isChecked()) !== selected) {
      await chip.click()
    }
  }

  listItemFor(daycare: Daycare) {
    return this.page.locator(`[data-qa="map-unit-list-${daycare.id}"]`)
  }

  async testMapPopup(daycare: Daycare) {
    await this.listItemFor(daycare).click()
    await this.map.markerFor(daycare).click()
    await waitUntilEqual(() => this.map.popupFor(daycare).name(), daycare.name)
  }
}

class Map {
  constructor(private readonly root: Locator) {}

  static readonly MAX_ZOOM_ATTEMPTS = 30
  readonly #zoomIn = this.root.locator('.leaflet-control-zoom-in')
  readonly #zoomOut = this.root.locator('.leaflet-control-zoom-out')

  readonly addressMarker = this.root.locator('[data-qa="map-marker-address"]')

  async zoomInDisabled(): Promise<boolean> {
    return await this.root.evaluate((el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  async zoomOutDisabled(): Promise<boolean> {
    return await this.#zoomOut.evaluate((el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  async zoomOut(times: number | null = null) {
    let attempts = times ?? Map.MAX_ZOOM_ATTEMPTS
    while (attempts > 0) {
      if (await this.zoomOutDisabled()) {
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
      if (await this.zoomInDisabled()) {
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

  markerFor(daycare: Daycare) {
    return this.root.locator(`[title="${daycare.name}"]`)
  }

  popupFor(daycare: Daycare): MapPopup {
    return new MapPopup(
      this.root.locator(`[data-qa="map-popup-${daycare.id}"]`)
    )
  }
}

class UnitDetailsPanel {
  constructor(private readonly root: Locator) {}

  readonly #name = this.root.locator('[data-qa="map-unit-details-name"]')
  readonly backButton = this.root.locator('[data-qa="map-unit-details-back"]')

  async waitFor() {
    await this.root.waitFor()
  }

  async name(): Promise<string | null> {
    return await this.#name.textContent()
  }
}

class MapPopup {
  constructor(private readonly root: Locator) {}

  readonly #name = this.root.locator('[data-qa="map-popup-name"]')
  readonly #noApplying = this.root.locator('[data-qa="map-popup-no-applying"]')

  async name(): Promise<string | null> {
    return await this.#name.textContent()
  }

  get noApplying(): Promise<string | null> {
    return this.#noApplying.textContent()
  }
}

class MapSearchInput {
  constructor(private readonly root: Locator) {}

  async fill(text: string) {
    await this.root.locator('input').fill(text)
  }

  async clickUnitResult(daycare: Daycare) {
    await this.root.locator(`[data-qa="map-search-${daycare.id}"]`).click()
  }

  async clickAddressResult(streetAddress: string) {
    await this.root
      .locator(
        `[data-qa="map-search-address"][data-address="${streetAddress}"]`
      )
      .click()
  }
}
