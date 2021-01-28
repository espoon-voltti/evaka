import { Selector, t } from 'testcafe'

export default class CitizenApplicationsPage {
  readonly childTitle = (childId: string) =>
    Selector(`[data-qa="title-applications-child-name-${childId}"]`)

  readonly applicationType = (applicationId: string) =>
    Selector(`[data-qa="title-application-type-${applicationId}"]`)

  readonly applicationUnit = (applicationId: string) =>
    Selector(`[data-qa="application-unit-${applicationId}"]`)

  readonly applicationPeriod = (applicationId: string) =>
    Selector(`[data-qa="application-period-${applicationId}"]`)

  readonly applicationCreated = (applicationId: string) =>
    Selector(`[data-qa="application-created-${applicationId}"]`)

  readonly applicationModified = (applicationId: string) =>
    Selector(`[data-qa="application-modified-${applicationId}"]`)

  readonly applicationStatus = (applicationId: string) =>
    Selector(`[data-qa="application-status-${applicationId}"]`)

  readonly removeApplicationButton = (applicationId: string) =>
    Selector(`[data-qa="button-remove-application-${applicationId}"]`)

  readonly modalOkBtn = Selector(`[data-qa="modal-okBtn"]`)
  readonly modalCancelBtn = Selector(`[data-qa="modal-cancelBtn"]`)

  async assertApplication(
    applicationId: string,
    expectedApplicationType: string,
    expectedUnitName: string,
    expectedPeriod: string,
    expectedStatus: string
  ) {
    await t
      .expect(this.applicationType(applicationId).textContent)
      .eql(expectedApplicationType)

    await t
      .expect(this.applicationUnit(applicationId).textContent)
      .eql(expectedUnitName)

    await t
      .expect(this.applicationPeriod(applicationId).textContent)
      .eql(expectedPeriod)

    await t
      .expect(this.applicationStatus(applicationId).textContent)
      .contains(expectedStatus)
  }

  async assertApplicationDoesNotExist(applicationId: string) {
    await t
      .expect(
        Selector(`[data-qa="title-application-type-${applicationId}"]`, {
          timeout: 2000
        }).visible
      )
      .notOk()
  }
}
