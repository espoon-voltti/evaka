import { Page } from 'playwright'
import { ElementSelector } from './index'

export class Radio extends ElementSelector {
  constructor(page: Page, selector: string) {
    super(page, selector)
  }

  get checked(): Promise<boolean> {
    return this.page.isChecked(`${this.selector} input`)
  }
}
