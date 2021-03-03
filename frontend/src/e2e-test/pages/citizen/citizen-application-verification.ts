// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
