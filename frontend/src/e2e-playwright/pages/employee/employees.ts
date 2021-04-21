import { Page } from 'playwright'
import { RawTextInput } from '../../utils/element'

export class EmployeesPage {
  constructor(private readonly page: Page) {}

  readonly nameInput = new RawTextInput(
    this.page,
    '[data-qa="employee-name-filter"]'
  )

  get visibleUsers(): Promise<string[]> {
    return this.page
      .$$('[data-qa="employee-name"]')
      .then((val) => Promise.all(val.map((ele) => ele.innerText())))
  }
}
