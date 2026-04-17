// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect } from '@playwright/test'
import type {
  ElementHandle,
  Keyboard,
  Locator,
  Request as PlaywrightRequest,
  Page as PlaywrightPage
} from '@playwright/test'

import type LocalDate from 'lib-common/local-date'

import { BoundingBox } from '.'

export type EnvType = 'desktop' | 'mobile'

export const envs = ['desktop', 'mobile'] as const

export class Page {
  readonly keyboard: Keyboard

  static async openNewTab(page: Page) {
    const newPage = await page.context.newPage()
    return new Page(newPage)
  }

  constructor(readonly page: PlaywrightPage) {
    this.keyboard = page.keyboard
  }

  get context() {
    return this.page.context()
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

  async goBack() {
    return this.page.goBack()
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

  async waitForUrl(url: string | RegExp) {
    return this.page.waitForURL(url)
  }

  // avoid using this if possible!
  async waitForTimeout(ms: number) {
    if (ms > 500) throw new Error('aint nobody got time for that')
    return this.page.waitForTimeout(ms)
  }

  async pause() {
    return this.page.pause()
  }

  async bringToFront() {
    return this.page.bringToFront()
  }

  onPopup(fn: (popup: Page) => void) {
    this.page.on('popup', (popup) => fn(new Page(popup)))
  }

  onRequest(fn: (req: PlaywrightRequest) => void) {
    this.page.on('request', fn)
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

  async assertTextsEqualAnyOrder(values: string[]) {
    const expected = values.slice().sort()
    await expect
      .poll(async () => {
        const texts = await this.allTexts()
        return texts.sort()
      })
      .toEqual(expected)
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
      .waitFor({ state: 'visible' })
      .then(() =>
        this.locator
          .boundingBox()
          .then((value) => (value ? new BoundingBox(value) : null))
      )
  }

  // Visible text content
  get text(): Promise<string> {
    return this.locator.innerText()
  }

  async assertText(assertion: (text: string) => boolean): Promise<void> {
    await expect.poll(() => this.text.then(assertion)).toBe(true)
  }

  get visible(): Promise<boolean> {
    return this.locator.isVisible()
  }

  get disabled(): Promise<boolean> {
    return this.locator
      .waitFor({ state: 'visible' })
      .then(() => this.locator.isDisabled())
  }

  async assertDisabled(disabled: boolean): Promise<void> {
    if (disabled) {
      await expect(this.locator).toBeDisabled()
    } else {
      await expect(this.locator).toBeEnabled()
    }
  }

  async hover(): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.hover()
  }

  async click(): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.click()
  }

  async getAttribute(name: string): Promise<string | null> {
    return this.locator.getAttribute(name)
  }

  async assertAttributeEquals(
    name: string,
    value: string | null
  ): Promise<void> {
    if (value === null) {
      await expect(this.locator).not.toHaveAttribute(name)
    } else {
      await expect(this.locator).toHaveAttribute(name, value)
    }
  }

  async hasAttribute(name: string): Promise<boolean> {
    return (await this.getAttribute(name)) !== null
  }

  async evaluate<R>(fn: (el: HTMLElement | SVGElement) => R): Promise<R> {
    return this.locator.evaluate(fn)
  }

  get focused(): Promise<boolean> {
    return this.locator.evaluate((el) => el === document.activeElement)
  }

  async assertFocused(focused: boolean): Promise<void> {
    if (focused) {
      await expect(this.locator).toBeFocused()
    } else {
      await expect(this.locator).not.toBeFocused()
    }
  }
}

export class TextInput extends Element {
  async type(text: string): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.type(text)
  }

  async fill(text: string): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.fill(text)
  }

  async blur(): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.blur()
  }

  async clear(): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.fill('')
  }

  async press(key: string): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.press(key)
  }

  get inputValue(): Promise<string> {
    return this.locator
      .waitFor({ state: 'visible' })
      .then(() => this.locator.inputValue())
  }
}

export class PinInput extends Element {
  async fill(pinCode: string): Promise<void> {
    await this.locator.waitFor({ state: 'visible' })
    await this.locator.pressSequentially(pinCode)
  }
}

export class DatePicker extends Element {
  #input = new TextInput(this)

  async fill(date: LocalDate | string) {
    const text = typeof date === 'string' ? date : date.format()
    await this.#input.fill(text)
    await this.#input.blur()
  }

  async clear() {
    await this.#input.clear()
  }

  async assertValueEquals(value: string) {
    await expect(this.#input.locator).toHaveValue(value)
  }
}

export class DateRangePicker extends Element {
  start = new DatePicker(this.findByDataQa('start-date'))
  end = new DatePicker(this.findByDataQa('end-date'))

  async fill(start: LocalDate | string, end: LocalDate | string) {
    await this.start.fill(start)
    await this.end.fill(end)
  }
}

export class FileInput extends Element {
  async setInputFiles(path: string | string[]) {
    await this.locator.setInputFiles(path)
  }
}

export const testFileName = 'test_file.png'
const assetsDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'assets')
export const testFilePath = join(assetsDir, testFileName)

export class FileUpload extends Element {
  #input = new FileInput(this.findByDataQa('btn-upload-file'))
  #uploadedFilesContainer = this.findByDataQa('uploaded-files')
  #uploadedFiles = this.#uploadedFilesContainer.findAllByDataQa('uploaded-file')

  async upload(path: string | string[]) {
    const fileCountBefore = await this.fileCount
    await this.#input.setInputFiles(path)
    await expect(
      this.#uploadedFilesContainer.find(
        `:nth-child(${fileCountBefore + 1}) [data-qa="file-download-button"]`
      ).locator
    ).toBeVisible()
  }

  async uploadTestFile() {
    await this.upload(testFilePath)
  }

  get fileCount(): Promise<number> {
    return this.#uploadedFiles.count()
  }

  async deleteUploadedFile(index = 0) {
    const fileCountBefore = await this.fileCount
    if (fileCountBefore < index) {
      throw new Error(
        `Cannot delete file ${index} because only ${fileCountBefore} files are uploaded`
      )
    }
    await this.#uploadedFiles
      .nth(index)
      .findByDataQa('file-delete-button')
      .click()
    await expect(this.#uploadedFiles.locator).toHaveCount(fileCountBefore - 1)
  }
}

export class AsyncButton extends Element {
  async status() {
    return this.getAttribute('data-status')
  }

  async waitUntilIdle() {
    await expect(this.locator).toHaveAttribute('data-status', '')
  }

  async waitUntilSuccess() {
    await expect(this.locator).toHaveAttribute('data-status', 'success')
  }
}

export class Checkable extends Element {
  readonly #input = this.locator.locator('input')

  async check() {
    await this.#input.check()
  }

  async uncheck() {
    await this.#input.uncheck()
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
    if (disabled) {
      await expect(this.#input).toBeDisabled()
    } else {
      await expect(this.#input).toBeEnabled()
    }
  }

  async waitUntilChecked(checked = true) {
    if (checked) {
      await expect(this.#input).toBeChecked()
    } else {
      await expect(this.#input).not.toBeChecked()
    }
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
    await expect(this.locator).toHaveAttribute(
      'aria-checked',
      checked ? 'true' : 'false'
    )
  }

  get disabled(): Promise<boolean> {
    return this.getAttribute('aria-disabled').then(
      (ariaDisabled) => ariaDisabled === 'true'
    )
  }

  async assertDisabled(disabled: boolean) {
    if (disabled) {
      await expect(this.locator).toHaveAttribute('aria-disabled', 'true')
    } else {
      await expect(this.locator).not.toHaveAttribute('aria-disabled')
    }
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

  async assertSelectedOption(value: string) {
    await expect(this.#input.locator).toHaveValue(value)
  }

  async assertOptions(expected: string[]) {
    await expect(this.#input.findAll('option').locator).toHaveText(expected)
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

  async assertOptions(expected: string[]) {
    const options = this.findAllByDataQa('item')
    await expect(options.locator).toHaveText(expected)
  }

  async selectItem(dataQa: string) {
    await this.click()
    await this.findByDataQa(dataQa).click()
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
    await this.locator.page().keyboard.press('Escape')
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
    await expect(this.findByDataQa('no-options').locator).toBeVisible()
  }

  async assertOptions(options: string[]) {
    const actualOptions = this.findAllByDataQa('option')
    await expect(actualOptions.locator).toHaveText(options)
  }

  async assertOptionsContain(options: string[]) {
    const actualOptions = this.findAllByDataQa('option')
    await expect(actualOptions.locator).toContainText(options)
  }
}

export class Collapsible extends Element {
  async isOpen() {
    await expect(this.locator).toHaveAttribute('data-status', /.*/)
    const status = await this.getAttribute('data-status')
    return status !== 'closed'
  }

  async open() {
    if (!(await this.isOpen())) {
      await this.click()
    }
    await expect(this.locator).not.toHaveAttribute('data-status', 'closed')
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
  labels = this.findByDataQa('select-recipient-tree').findAll('label')

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

  optionByLabel(label: string): Element {
    return this.labels.find(`text=${label}`)
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
    await expect(this.locator).toHaveAttribute('data-qa-status', status)
  }
}

export class SecondaryRecipient extends Element {
  async assertIsSelected() {
    await expect(this.locator).toHaveAttribute('aria-checked', 'true')
  }
  async assertIsUnselected() {
    await expect(this.locator).toHaveAttribute('aria-checked', 'false')
  }
}
