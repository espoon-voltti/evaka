// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'

export class ApplicationDetailsPage {
  readonly guardianName = Selector('[data-qa="guardian-name"]')

  readonly vtjGuardianName = Selector('[data-qa="vtj-guardian-name"]')

  readonly otherGuardianAgreementStatus = Selector(
    '[data-qa="agreement-status"]'
  )

  readonly otherGuardianSameAddress = Selector(
    '[data-qa="other-vtj-guardian-lives-in-same-address"]'
  )

  readonly noOtherVtjGuardianText = Selector(
    '[data-qa="no-other-vtj-guardian"]'
  )

  readonly applicationStatus = Selector('[data-qa="application-status"]')
}
