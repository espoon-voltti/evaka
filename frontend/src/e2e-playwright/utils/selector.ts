import { Page } from 'playwright'
import { BoundingBox } from '.'

export class ElementSelector {
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

  get visible(): Promise<boolean> {
    return this.page.isVisible(this.selector)
  }

  async click(): Promise<void> {
    await this.page.click(this.selector)
  }
}

type Constructor<T extends ElementSelector> = new (...args: any[]) => T
const identity = <T>(x: T) => x
export const descendantInput = (selector: string) => `${selector} input`

export const Checked = <T extends Constructor<ElementSelector>>(
  Base: T,
  inputSelector: (selector: string) => string = identity
) =>
  class Checked extends Base {
    readonly #input = inputSelector(this.selector)

    get checked(): Promise<boolean> {
      return this.page.isChecked(this.#input)
    }
  }

export const TextInput = <T extends Constructor<ElementSelector>>(
  Base: T,
  inputSelector: (selector: string) => string = identity
) =>
  class TextInput extends Base {
    readonly #input = inputSelector(this.selector)

    async type(text: string): Promise<void> {
      await this.page.type(this.#input, text)
    }
  }

export default {
  Element: ElementSelector,
  Radio: Checked(ElementSelector, descendantInput),
  SelectionChip: Checked(ElementSelector, descendantInput)
}
