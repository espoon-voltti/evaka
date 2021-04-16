import { Page } from 'playwright'
import { RawElement } from 'e2e-playwright/utils/element'

export default class ChildInformationPage {
  constructor(private readonly page: Page) {}

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector, section } = collapsibles[collapsible]
    const element = new RawElement(this.page, selector)
    await element.click()
    return new section(element) as SectionFor<C>
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

const collapsibles = {
  dailyServiceTimes: {
    selector: '[data-qa="child-daily-service-times-collapsible"]',
    section: DailyServiceTimeSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
