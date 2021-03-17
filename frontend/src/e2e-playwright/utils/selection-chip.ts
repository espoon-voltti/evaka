import { Page } from 'playwright'
import { ElementSelector } from './index'

export class SelectionChip extends ElementSelector {
  readonly #input = `${this.selector} input`

  constructor(page: Page, selector: string) {
    super(page, selector)
  }

  get selected(): Promise<boolean> {
    return this.page.isChecked(this.#input)
  }
}
