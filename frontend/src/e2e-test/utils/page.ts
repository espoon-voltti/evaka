// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  BrowserContextOptions,
  Keyboard,
  Locator,
  Page as PlaywrightPage
} from 'playwright'

import { newBrowserContext } from '../browser'

import { BoundingBox, waitUntilDefined, waitUntilEqual, waitUntilTrue } from '.'

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

  async capturePopup(fn: () => Promise<void>): Promise<Page> {
    const popup = new Promise<Page>((resolve) => this.onPopup(resolve))
    await fn()
    return popup
  }

  find(selector: string) {
    return new Element(this.page.locator(selector))
  }

  findText(text: string | RegExp) {
    return new Element(this.page.locator(`text=${text.toString()}`))
  }

  findTextExact(text: string) {
    return new Element(this.page.locator(`text="${text}"`))
  }

  findAll(selector: string) {
    return new ElementCollection(this.page.locator(selector))
  }

  findByDataQa(dataQa: string) {
    return this.find(`[data-qa="${dataQa}"]`)
  }

  findAllByDataQa(dataQa: string) {
    return this.findAll(`[data-qa="${dataQa}"]`)
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

  async assertCount(count: number): Promise<void> {
    if (count === 0) {
      await this.nth(0).waitUntilHidden()
    } else {
      await this.nth(count - 1).waitUntilVisible()
      await this.nth(count).waitUntilHidden()
    }
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

  findByDataQa(dataQa: string): Element {
    return new Element(this.locator.locator(`[data-qa="${dataQa}"]`))
  }

  findText(text: string | RegExp): Element {
    return new Element(this.locator.locator(`text=${text.toString()}`))
  }

  findTextExact(text: string): Element {
    return new Element(this.locator.locator(`text="${text}"`))
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

  async hover(): Promise<void> {
    await this.locator.hover()
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

  async hasAttribute(name: string): Promise<boolean> {
    return (await this.getAttribute(name)) !== null
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

  async press(key: string): Promise<void> {
    await this.locator.press(key)
  }

  get inputValue(): Promise<string> {
    return this.locator.inputValue()
  }
}

export class DatePicker extends Element {
  #input = new TextInput(this)

  async fill(text: string) {
    await this.#input.fill(text)
    await this.#input.press('Enter')
  }
}

export class DatePickerDeprecated extends Element {
  #input = new TextInput(this.find('input'))

  async fill(text: string) {
    await this.#input.fill(text)
    await this.#input.press('Enter')
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

  async waitUntilIdle() {
    await waitUntilEqual(() => this.status(), '')
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
  #input = this.find('select')

  async selectOption(
    value: string | string[] | { value: string } | { label: string }
  ) {
    await this.#input.locator.selectOption(value)
  }

  get selectedOption(): Promise<string> {
    return this.#input.locator.inputValue()
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

export class MultiSelect extends Element {
  #input = new TextInput(this.find('input[type="text"]'))

  async fill(text: string) {
    await this.#input.fill(text)
  }

  async selectItem(text: string, value: string) {
    await this.find(`[data-qa="option"][data-id="id-${value}"]`).click()
  }

  async close() {
    await this.#input.click()
  }

  async fillAndSelectFirst(text: string) {
    await this.#input.fill(text)
    await this.findAll(`[data-qa="option"]`).first().click()
    await this.close()
  }
}

export class Collapsible extends Element {
  async isOpen() {
    const status = await waitUntilDefined(() =>
      this.getAttribute('data-status')
    )
    return status !== 'closed'
  }

  async open() {
    await waitUntilTrue(async () => {
      if (!(await this.isOpen())) {
        await this.click()
      }
      return this.isOpen()
    })
  }
}

export class Modal extends Element {
  #closeButton = this.find('[data-qa="modal-close"]')
  #submitButton = this.find('[data-qa="modal-okBtn"]')

  async close() {
    await this.#closeButton.click()
  }

  async submit() {
    await this.#submitButton.click()
  }
}
