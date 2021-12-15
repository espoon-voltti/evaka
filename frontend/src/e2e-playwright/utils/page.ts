// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  BrowserContextOptions,
  Keyboard,
  Locator,
  Page as PlaywrightPage
} from 'playwright'
import { BoundingBox, waitUntilDefined, waitUntilEqual, waitUntilTrue } from '.'
import { newBrowserContext } from '../browser'

export class Page {
  readonly keyboard: Keyboard

  static async open(options?: BrowserContextOptions & { mockedTime?: Date }) {
    const ctx = await newBrowserContext(options)
    const page = await ctx.newPage()
    return new Page(page)
  }

  private constructor(readonly page: PlaywrightPage) {
    this.keyboard = page.keyboard
  }

  get url() {
    return this.page.url()
  }

  async goto(url: string) {
    return this.page.goto(url)
  }

  async reload() {
    return this.page.reload()
  }

  async close() {
    return this.page.close()
  }

  async waitForDownload() {
    return this.page.waitForEvent('download')
  }

  async pause() {
    return this.page.pause()
  }

  onPopup(fn: (popup: Page) => void) {
    this.page.on('popup', (popup) => fn(new Page(popup)))
  }

  find(selector: string) {
    return new Element(this.page.locator(selector))
  }

  findAll(selector: string) {
    return new ElementCollection(this.page.locator(selector))
  }
}

export class ElementCollection {
  constructor(readonly locator: Locator) {}

  first() {
    return new Element(this.locator.first())
  }

  /**
   * @param n Zero-based index
   */
  nth(n: number) {
    return new Element(this.locator.nth(n))
  }

  async count(): Promise<number> {
    return this.locator.count()
  }

  async allInnerTexts(): Promise<string[]> {
    return this.locator.allInnerTexts()
  }

  async evaluateAll<R>(
    fn: (el: (HTMLElement | SVGElement)[]) => R
  ): Promise<R> {
    return this.locator.evaluateAll(fn)
  }

  elem() {
    return new Element(this.locator)
  }

  find(selector: string) {
    return new Element(this.locator.locator(selector))
  }

  findAll(selector: string) {
    return new ElementCollection(this.locator.locator(selector))
  }
}

export class Element {
  readonly locator: Locator

  constructor(value: Element | Locator) {
    if (value instanceof Element) {
      this.locator = value.locator
    } else {
      this.locator = value
    }
  }

  find(selector: string): Element {
    return new Element(this.locator.locator(selector))
  }

  findAll(selector: string): ElementCollection {
    return new ElementCollection(this.locator.locator(selector))
  }

  get boundingBox(): Promise<BoundingBox | null> {
    return this.locator
      .boundingBox()
      .then((value) => (value ? new BoundingBox(value) : null))
  }

  get innerText(): Promise<string> {
    return this.locator.innerText()
  }

  get textContent(): Promise<string | null> {
    return this.locator.textContent()
  }

  get visible(): Promise<boolean> {
    return this.locator.isVisible()
  }

  get disabled(): Promise<boolean> {
    return this.locator.isDisabled()
  }

  async click(): Promise<void> {
    await this.locator.click()
  }

  async waitUntilVisible(): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
  }

  async waitUntilHidden(): Promise<void> {
    await this.locator.waitFor({ state: 'hidden' })
  }

  async getAttribute(name: string): Promise<string | null> {
    return this.locator.getAttribute(name)
  }

  async evaluate<R>(fn: (el: HTMLElement | SVGElement) => R): Promise<R> {
    return this.locator.evaluate(fn)
  }
}

export class TextInput extends Element {
  async type(text: string): Promise<void> {
    await this.locator.type(text)
  }

  async fill(text: string): Promise<void> {
    await this.locator.fill(text)
  }

  async clear(): Promise<void> {
    await this.locator.fill('')
  }

  get inputValue(): Promise<string> {
    return this.locator.inputValue()
  }
}

export class DatePickerDeprecated extends Element {
  #input = new TextInput(this.find('input'))
  #datepicker = this.locator.locator('.react-datepicker-popper')

  async fill(text: string) {
    await this.#input.fill(text)
    await this.closeDatePicker()
  }

  async closeDatePicker() {
    await this.#datepicker.waitFor()
    const placement = await this.#datepicker.getAttribute('data-placement')
    const box = await this.#datepicker.boundingBox()
    if (!placement || !box) throw new Error('Datepicker not open')

    const clickAt = {
      x: box.width / 2,
      y: placement === 'bottom' ? 0 : box.height - 1
    }
    await this.#datepicker.click({ position: clickAt })
  }
}

export class FileInput extends Element {
  async setInputFiles(path: string | string[]) {
    await this.locator.setInputFiles(path)
  }
}

export class AsyncButton extends Element {
  async status() {
    return this.getAttribute('data-status')
  }

  async waitUntilSuccessful() {
    await waitUntilEqual(() => this.status(), 'success')
  }
}

export class Checkable extends Element {
  readonly #input = this.locator.locator('input')

  // check and uncheck are forced because an <svg> element hides the <input>
  // and intercepts pointer events

  async check() {
    await this.#input.check({ force: true })
  }

  async uncheck() {
    await this.#input.uncheck({ force: true })
  }

  get checked(): Promise<boolean> {
    return this.#input.isChecked()
  }

  async waitUntilChecked(checked = true) {
    await waitUntilEqual(() => this.checked, checked)
  }
}

export class Checkbox extends Checkable {}

export class Radio extends Checkable {}

export class SelectionChip extends Checkable {}

export class Select extends Element {
  async selectOption(
    value: string | string[] | { value: string } | { label: string }
  ) {
    await this.locator.selectOption(value)
  }
}

export class Combobox extends Element {
  #input = new TextInput(this.find('input'))

  async fill(text: string) {
    await this.#input.fill(text)
  }

  async fillAndSelectFirst(text: string) {
    await this.#input.fill(text)
    await this.findAll(`[data-qa="item"]`).first().click()
  }

  async fillAndSelectItem(text: string, value: string) {
    await this.#input.fill(text)
    await this.find(`[data-qa="value-${value}"]`).click()
  }
}

export class Collapsible extends Element {
  #trigger = this.find('[data-qa="collapsible-trigger"]')

  async isOpen() {
    const status = await waitUntilDefined(() =>
      this.getAttribute('data-status')
    )
    return status !== 'closed'
  }

  async open() {
    await waitUntilTrue(async () => {
      if (!(await this.isOpen())) {
        await this.#trigger.click()
      }
      return this.isOpen()
    })
  }
}
