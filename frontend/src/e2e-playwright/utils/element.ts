// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { BoundingBox } from '.'

export class RawElement {
  constructor(public page: Page, public selector: string) {}

  /**
   * Returns the client-side bounding box of this element.
   *
   * If the element does not exist on the page, an error is thrown
   */
  get boundingBox(): Promise<BoundingBox> {
    return this.page
      .$eval(this.selector, (el) => {
        // DOMRect doesn't serialize nicely, so extract the individual fields
        const rect = el.getBoundingClientRect()
        const { x, y, width, height } = rect
        return { x, y, width, height }
      })
      .then((box) => new BoundingBox(box))
  }

  get innerText(): Promise<string> {
    return this.page.innerText(this.selector)
  }

  get visible(): Promise<boolean> {
    return this.page.isVisible(this.selector)
  }

  get disabled(): Promise<boolean> {
    return this.page.isDisabled(this.selector)
  }

  async click(): Promise<void> {
    await this.page.click(this.selector)
  }

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForSelector(this.selector, { state: 'visible' })
  }

  find(descendant: string): RawElement {
    return new RawElement(this.page, `${this.selector} ${descendant}`)
  }

  findInput(descendant: string): RawTextInput {
    return new RawTextInput(this.page, `${this.selector} ${descendant}`)
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Constructor<T extends RawElement> = new (...args: any[]) => T
const identity = <T>(x: T) => x
export const descendantInput = (selector: string) => `${selector} input`

export const WithChecked = <T extends Constructor<RawElement>>(
  Base: T,
  inputSelector: (selector: string) => string = identity
) =>
  class Checked extends Base {
    readonly #input = inputSelector(this.selector)

    get checked(): Promise<boolean> {
      return this.page.isChecked(this.#input)
    }
  }

export const WithTextInput = <T extends Constructor<RawElement>>(
  Base: T,
  inputSelector: (selector: string) => string = identity
) =>
  class TextInput extends Base {
    readonly #input = inputSelector(this.selector)

    async type(text: string): Promise<void> {
      await this.page.type(this.#input, text)
    }

    async fill(text: string): Promise<void> {
      await this.page.fill(this.#input, text)
    }

    async clear(): Promise<void> {
      await this.page.click(this.#input, { clickCount: 3 })
      await this.page.keyboard.press('Backspace')
    }
  }

export class Radio extends WithChecked(RawElement, descendantInput) {}

export class RawTextInput extends WithTextInput(RawElement) {}
export class RawRadio extends WithChecked(RawElement) {}

export class SelectionChip extends WithChecked(RawElement, descendantInput) {}
