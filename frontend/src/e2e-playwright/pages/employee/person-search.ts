import { Page } from 'playwright'
import LocalDate from 'lib-common/local-date'
import { RawElement, RawTextInput } from 'e2e-playwright/utils/element'
import { waitUntilEqual } from 'e2e-playwright/utils'

export default class PersonSearchPage {
  constructor(private readonly page: Page) {}

  #searchInput = new RawTextInput(this.page, '[data-qa="search-input"]')
  #personLink = new RawElement(this.page, '[data-qa="person-row"] a')
  #createPersonButton = new RawElement(
    this.page,
    '[data-qa="create-person-button"]'
  )
  #createPersonModal = {
    firstNameInput: new RawTextInput(this.page, '[data-qa="first-name-input"]'),
    lastNameInput: new RawTextInput(this.page, '[data-qa="last-name-input"]'),
    dateOfBirthInput: new RawTextInput(
      this.page,
      '[data-qa="date-of-birth-input"]'
    ),
    streetAddressInput: new RawTextInput(
      this.page,
      '[data-qa="street-address-input"]'
    ),
    postalCodeInput: new RawTextInput(
      this.page,
      '[data-qa="postal-code-input"]'
    ),
    postOfficeInput: new RawTextInput(
      this.page,
      '[data-qa="post-office-input"]'
    )
  }
  #personData = {
    firstName: new RawElement(this.page, '[data-qa="person-first-names"]'),
    lastName: new RawElement(this.page, '[data-qa="person-last-name"]'),
    dateOfBirth: new RawElement(this.page, '[data-qa="person-birthday"]'),
    address: new RawElement(this.page, '[data-qa="person-address"]'),
    ssn: new RawElement(this.page, '[data-qa="person-ssn"]')
  }
  #ssnInput = new RawTextInput(this.page, '[data-qa="ssn-input"]')
  #modalConfirm = new RawElement(this.page, '[data-qa="modal-okBtn"]')

  async createPerson(personData: {
    firstName: string
    lastName: string
    dateOfBirth: LocalDate
    streetAddress: string
    postalCode: string
    postOffice: string
  }) {
    await this.#createPersonButton.click()
    await this.#createPersonModal.firstNameInput.type(personData.firstName)
    await this.#createPersonModal.lastNameInput.type(personData.lastName)
    await this.#createPersonModal.dateOfBirthInput.type(
      personData.dateOfBirth.format()
    )
    await this.#createPersonModal.streetAddressInput.type(
      personData.streetAddress
    )
    await this.#createPersonModal.postalCodeInput.type(personData.postalCode)
    await this.#createPersonModal.postOfficeInput.type(personData.postOffice)
    await this.#modalConfirm.click()
  }

  async findPerson(searchString: string) {
    await this.#searchInput.clear()
    await this.#searchInput.type(searchString)
    await this.#personLink.click()
  }

  async assertPersonData(personData: {
    firstName: string
    lastName: string
    dateOfBirth: LocalDate
    streetAddress: string
    postalCode: string
    postOffice: string
    ssn?: string
  }) {
    await waitUntilEqual(
      () => this.#personData.firstName.innerText,
      personData.firstName
    )
    await waitUntilEqual(
      () => this.#personData.lastName.innerText,
      personData.lastName
    )
    await waitUntilEqual(
      () => this.#personData.dateOfBirth.innerText,
      personData.dateOfBirth.format()
    )
    await waitUntilEqual(
      () => this.#personData.address.innerText,
      `${personData.streetAddress}, ${personData.postalCode} ${personData.postOffice}`
    )
    if (personData.ssn === undefined) {
      await this.#personData.ssn.find('button').waitUntilVisible()
      await waitUntilEqual(
        () => this.#personData.ssn.find('button').innerText,
        'Aseta hetu'
      )
    } else {
      await waitUntilEqual(() => this.#personData.ssn.innerText, personData.ssn)
    }
  }

  async addSsn(ssn: string) {
    await this.#personData.ssn.find('button').click()
    await this.#ssnInput.type(ssn)
    await this.#modalConfirm.click()
  }
}
