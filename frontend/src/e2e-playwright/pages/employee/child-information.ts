import { Page } from 'playwright'
import { RawElement } from 'e2e-playwright/utils/element'

export type Collapsible = 'dailyServiceTimes'

export default class ChildInformationPage {
  constructor(private readonly page: Page) {}

  readonly #collapsibles: Record<Collapsible, RawElement> = {
    dailyServiceTimes: new RawElement(
      this.page,
      '[data-qa="child-daily-service-times-collapsible"]'
    )
  }

  async openCollapsible(collapsible: Collapsible) {
    const element = this.#collapsibles[collapsible]
    await element.click()
    return element
  }

  async openDailyServiceTimesCollapsible() {
    const element = await this.openCollapsible('dailyServiceTimes')
    return new DailyServiceTimeSection(element)
  }
}

export class DailyServiceTimeSection {
  constructor(private section: RawElement) {}

  readonly #typeText = this.section.find('[data-qa="times-type"]')
  readonly #timesText = this.section.find('[data-qa="times"]')
  readonly #editButton = this.section.find('[data-qa="edit-button"]')

  get typeText(): Promise<string> {
    return this.#typeText.innerText
  }

  get timesText(): Promise<string> {
    return this.#timesText.innerText
  }

  get hasTimesText(): Promise<boolean> {
    return this.#timesText.visible
  }

  async edit() {
    await this.#editButton.click()
    return new DailyServiceTimesSectionEdit(this.section)
  }
}

export class DailyServiceTimesSectionEdit {
  readonly #notSetRadio = this.section.find('[data-qa="radio-not-set"]')

  readonly #regularRadio = this.section.find('[data-qa="radio-regular"]')
  readonly #irregularRadio = this.section.find('[data-qa="radio-irregular"]')
  readonly #submitButton = this.section.find('[data-qa="submit-button"]')

  constructor(private section: RawElement) {}

  async selectTimeNotSet() {
    await this.#notSetRadio.click()
  }

  async selectRegularTime() {
    await this.#regularRadio.click()
  }

  async selectIrregularTime() {
    await this.#irregularRadio.click()
  }

  async fillTimeRange(which: string, start: string, end: string) {
    await this.section.findInput(`[data-qa="${which}-start"]`).fill(start)
    await this.section.findInput(`[data-qa="${which}-end"]`).fill(end)
  }

  async selectDay(day: string) {
    await this.section.find(`[data-qa="${day}-checkbox"]`).click()
  }

  get submitIsDisabled(): Promise<boolean> {
    return this.#submitButton.disabled
  }

  async submit() {
    await this.#submitButton.click()
  }
}
