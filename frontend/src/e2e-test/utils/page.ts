// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  BrowserContextOptions,
  ElementHandle,
  Keyboard,
  Locator,
  Page as PlaywrightPage
} from 'playwright'

import LocalDate from 'lib-common/local-date'

import { EvakaBrowserContextOptions, newBrowserContext } from '../browser'

import { BoundingBox, waitUntilDefined, waitUntilEqual, waitUntilTrue } from '.'

export class Page {
  readonly keyboard: Keyboard

  static async open(
    options?: BrowserContextOptions & EvakaBrowserContextOptions
  ) {
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

  async waitForPopup() {
    return this.page.waitForEvent('popup')
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

  find(selector: string, options?: { hasText?: string | RegExp }) {
    return new Element(this.page.locator(selector, options))
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

  last() {
    return new Element(this.locator.last())
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

  async allTexts(): Promise<string[]> {
    return this.locator.allInnerTexts()
  }

  async elementHandles(): Promise<ElementHandle<Node>[]> {
    return this.locator.elementHandles()
  }

  async evaluateAll<R>(
    fn: (el: (HTMLElement | SVGElement)[]) => R
  ): Promise<R> {
    return this.locator.evaluateAll(fn)
  }

  only() {
    return new Element(this.locator)
  }

  find(selector: string) {
    return new Element(this.locator.locator(selector))
  }

  findByDataQa(dataQa: string) {
    return this.find(`[data-qa="${dataQa}"]`)
  }

  findAll(selector: string) {
    return new ElementCollection(this.locator.locator(selector))
  }

  findAllByDataQa(dataQa: string) {
    return this.findAll(`[data-qa="${dataQa}"]`)
  }

  async assertCount(count: number): Promise<void> {
    if (count === 0) {
      await this.nth(0).waitUntilHidden()
    } else {
      await this.nth(count - 1).waitUntilVisible()
      await this.nth(count).waitUntilHidden()
    }
  }

  async assertTextsEqual(values: string[]) {
    await waitUntilEqual(() => this.allTexts(), values)
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

  find(selector: string, options?: { hasText?: string | RegExp }): Element {
    return new Element(this.locator.locator(selector, options))
  }

  findByDataQa(dataQa: string): Element {
    return new Element(this.locator.locator(`[data-qa="${dataQa}"]`))
  }

  findAllByDataQa(dataQa: string): ElementCollection {
    return new ElementCollection(this.locator.locator(`[data-qa="${dataQa}"]`))
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

  // Visible text content
  get text(): Promise<string> {
    return this.locator.innerText()
  }

  async assertText(assertion: (text: string) => boolean): Promise<void> {
    await waitUntilTrue(async () => assertion(await this.text))
  }

  async assertTextEquals(expected: string): Promise<void> {
    await waitUntilEqual(() => this.text, expected)
  }

  get visible(): Promise<boolean> {
    return this.locator.isVisible()
  }

  get disabled(): Promise<boolean> {
    return this.locator.isDisabled()
  }

  async assertDisabled(disabled: boolean): Promise<void> {
    await waitUntilEqual(() => this.disabled, disabled)
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

  async assertAttributeEquals(
    name: string,
    value: string | null
  ): Promise<void> {
    await waitUntilEqual(() => this.getAttribute(name), value)
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

  async blur(): Promise<void> {
    await this.locator.blur()
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

  async assertValueEquals(expectedValue: string): Promise<void> {
    await waitUntilEqual(() => this.inputValue, expectedValue)
  }
}

export class PinInput extends Element {
  async fill(pinCode: string): Promise<void> {
    await this.locator.pressSequentially(pinCode)
  }
}

export class DatePicker extends Element {
  #input = new TextInput(this)

  async fill(text: string) {
    await this.#input.fill(text)
    await this.#input.press('Escape')
  }
}

export class DatePickerDeprecated extends Element {
  #input = new TextInput(this.find('input'))

  async fill(text: string | LocalDate) {
    await this.#input.fill(typeof text === 'string' ? text : text.format())
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

  async waitUntilSuccess() {
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

  async toggle() {
    if (await this.checked) {
      await this.uncheck()
    } else {
      await this.check()
    }
  }

  async assertDisabled(disabled: boolean) {
    await waitUntilEqual(() => this.#input.isDisabled(), disabled)
  }

  async waitUntilChecked(checked = true) {
    await waitUntilEqual(() => this.checked, checked)
  }
}

export class Checkbox extends Checkable {}

export class Radio extends Checkable {
  get disabled(): Promise<boolean> {
    return this.find('input[type=radio]').disabled
  }
}

export class SelectionChip extends Element {
  async check() {
    if (!(await this.checked)) {
      await this.click()
      await this.waitUntilChecked(true)
    }
  }

  async uncheck() {
    if (await this.checked) {
      await this.click()
      await this.waitUntilChecked(false)
    }
  }

  get checked(): Promise<boolean> {
    return this.getAttribute('aria-checked').then(
      (ariaChecked) => ariaChecked === 'true'
    )
  }

  async waitUntilChecked(checked = true) {
    await waitUntilEqual(() => this.checked, checked)
  }
}

export class Select extends Element {
  #input = this.find('select')

  async selectOption(
    value:
      | string
      | string[]
      | { value: string }
      | { label: string }
      | { index: number }
  ) {
    await this.#input.locator.selectOption(value)
  }

  get selectedOption(): Promise<string> {
    return this.#input.locator.inputValue()
  }

  get allOptions(): Promise<string[]> {
    return this.#input.findAll('option').allTexts()
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

  async fillAndSelectItem(text: string, itemDataQa: string) {
    await this.#input.fill(text)
    await this.find(`[data-qa="item"]`)
      .find(`[data-qa="${itemDataQa}"]`)
      .click()
  }
}

export class MultiSelect extends Element {
  #input = new TextInput(this.find('input[type="text"]'))

  async fill(text: string) {
    await this.#input.fill(text)
  }

  async selectItem(value: string) {
    await this.find(`[data-qa="option"][data-id="${value}"]`).click()
  }

  async close() {
    await this.#input.click()
  }

  async selectFirst() {
    await this.click()
    await this.findAllByDataQa('option').first().click()
  }

  async fillAndSelectFirst(text: string) {
    await this.#input.fill(text)
    await this.findAllByDataQa('option').first().click()
    await this.close()
  }

  async assertNoOptions() {
    await this.findByDataQa('no-options').waitUntilVisible()
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
  closeButton = this.find('[data-qa="modal-close"]')
  submitButton = this.find('[data-qa="modal-okBtn"]')

  async close() {
    await this.closeButton.click()
  }

  async submit() {
    await this.submitButton.click()
  }
}

export class TreeDropdown extends Element {
  values = this.findByDataQa('selected-values').findAllByDataQa('value')

  private async expanded(): Promise<boolean> {
    return (
      (await this.findByDataQa('tree-dropdown').getAttribute(
        'data-qa-expanded'
      )) === 'true'
    )
  }

  async open(): Promise<void> {
    if (!(await this.expanded())) {
      await this.click()
    }
  }

  async close(): Promise<void> {
    if (await this.expanded()) {
      await this.click()
    }
  }

  option(key: string): Checkbox {
    return new Checkbox(this.findByDataQa(`tree-checkbox-${key}`))
  }

  firstOption(): Checkbox {
    return new Checkbox(this.findAll(`[data-qa*="tree-checkbox-"]`).nth(0))
  }

  async expandOption(key: string): Promise<void> {
    await this.findByDataQa(`tree-toggle-${key}`).click()
  }

  async expandAll() {
    const toggles = await this.findAll(
      '[data-qa*="tree-toggle-"]'
    ).elementHandles()
    for (const toggle of toggles) {
      if ((await toggle.getAttribute('aria-expanded')) === 'false') {
        await toggle.click()
      }
    }
  }
}

export class StaticChip extends Element {
  get status(): Promise<string | null> {
    return this.getAttribute('data-qa-status')
  }

  async assertStatus(status: string) {
    await waitUntilEqual(() => this.status, status)
  }
}
