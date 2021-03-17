import { Page } from 'playwright'
import { Radio } from '../../utils/radio'
import { Daycare } from 'e2e-test-common/dev-api/types'
import { SelectionChip } from '../../utils/selection-chip'
import { delay, ElementSelector } from '../../utils'

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

  async setLanguageFilter(language: 'fi' | 'sv', selected: boolean) {
    const chip = new SelectionChip(
      this.page,
      `[data-qa="map-filter-${language}"]`
    )
    if ((await chip.selected) !== selected) {
      await chip.click()
    }
  }

  listItemFor(daycare: Daycare): MapUnitListItem {
    return new MapUnitListItem(
      this.page,
      `[data-qa="map-unit-list-${daycare.id}"]`
    )
  }
}

class MapUnitListItem {
  constructor(private readonly page: Page, private readonly selector: string) {}

  get visible(): Promise<boolean> {
    return this.page.isVisible(this.selector)
  }

  async click(): Promise<void> {
    await this.page.click(this.selector)
  }
}

class Map extends ElementSelector {
  static readonly MAX_ZOOM_ATTEMPTS = 30
  readonly #zoomIn = `${this.selector} .leaflet-control-zoom-in`
  readonly #container = new ElementSelector(
    this.page,
    `${this.selector} .leaflet-container`
  )
  readonly addressMarker = new ElementSelector(
    this.page,
    `${this.selector} [data-qa="map-marker-address"]`
  )

  get zoomInDisabled(): Promise<boolean> {
    return this.page.$eval(this.#zoomIn, (el) =>
      el.classList.contains('leaflet-disabled')
    )
  }

  async zoomInFully() {
    let attempts = 0
    while (attempts < Map.MAX_ZOOM_ATTEMPTS) {
      if (await this.zoomInDisabled) {
        return
      }
      await this.page.click(this.#zoomIn)
      attempts++
      await delay(100)
    }
    await this.page.pause()
    throw new Error(`Failed to zoom in after ${attempts} attempts`)
  }

  markerFor(daycare: Daycare): MapMarker {
    return new MapMarker(this.page, `[data-qa="map-marker-${daycare.id}"]`)
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

class UnitDetailsPanel extends ElementSelector {
  readonly #name = `${this.selector} [data-qa="map-unit-details-name"]`
  readonly backButton = new ElementSelector(
    this.page,
    `${this.selector} [data-qa="map-unit-details-back"]`
  )

  get name(): Promise<string | null> {
    return this.page.textContent(this.#name)
  }
}

class MapMarker extends ElementSelector {}

class MapPopup extends ElementSelector {
  readonly #name = `${this.selector} [data-qa="map-popup-name"]`

  get name(): Promise<string | null> {
    return this.page.textContent(this.#name)
  }
}

class MapSearchInput {
  readonly #input = `${this.selector} input`
  constructor(private readonly page: Page, private readonly selector: string) {}

  async typeText(text: string) {
    await this.page.type(this.#input, text)
  }

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
