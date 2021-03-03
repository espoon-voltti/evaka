// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { t, ClientFunction } from 'testcafe'
import * as testcafe from 'testcafe'

export const scrollTo = ClientFunction((x: number, y: number) => {
  window.scrollTo(x, y)
})

export async function scrollThenClick(t: TestController, s: Selector) {
  await scrollTo(0, (await s.boundingClientRect).bottom)
  await t.click(s)
}

export const waitUntilScrolled = ClientFunction(
  (timeout = 5000) =>
    new Promise((resolve) => {
      const handler = () => {
        window.removeEventListener('scroll', handler)
        resolve()
      }

      window.addEventListener('scroll', handler)
      setTimeout(handler, timeout)
    })
)

export class Checkbox {
  private _input: Selector

  constructor(private readonly selector: Selector) {
    this._input = selector.find('input')
  }

  async click(): Promise<void> {
    await testcafe.t.expect(this.disabled).eql(false)
    await scrollTo(0, (await this.selector.boundingClientRect).bottom)
    await testcafe.t.click(this.selector)
  }

  get checked(): Promise<boolean> {
    // cast needed because checked is Promise<boolean | undefined>
    return (this._input.checked as unknown) as Promise<boolean>
  }

  get disabled(): Promise<boolean> {
    return this._input.hasAttribute('disabled')
  }

  get exists(): Promise<boolean> {
    return this.selector.exists
  }
}

export class SelectionChip {
  private _input: Selector

  constructor(private readonly selector: Selector) {
    this._input = selector.find('input')
  }

  async click(): Promise<void> {
    await scrollTo(0, (await this.selector.boundingClientRect).bottom)
    await testcafe.t.click(this.selector)
  }

  get selected(): Promise<boolean> {
    // cast needed because checked is Promise<boolean | undefined>
    return (this._input.checked as unknown) as Promise<boolean>
  }

  get exists(): Promise<boolean> {
    return this.selector.exists
  }
}

export const selectFirstOption = async (
  container: Selector,
  searchString: string
) => {
  await t.click(container)
  const input = container.find('input')
  await t.typeText(input, searchString)
  await t.click(container.find('[id*="-option-"]'))
}
