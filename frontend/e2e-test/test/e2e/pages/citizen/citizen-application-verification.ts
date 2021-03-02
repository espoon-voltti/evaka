import { Selector } from 'testcafe'

export default class CitizenApplicationVerificationPage {
  readonly serviceNeedUrgencyAttachmentDownload = (filename: string) =>
    Selector('[data-qa="service-need-urgency-attachment-download"]').withText(
      filename
    )
  readonly serviceNeedShiftCareAttachmentDownload = (filename: string) =>
    Selector(
      '[data-qa="service-need-shift-care-attachment-download"]'
    ).withText(filename)
}
