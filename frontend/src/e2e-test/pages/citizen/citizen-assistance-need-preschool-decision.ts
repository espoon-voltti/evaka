// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class AssistanceNeedPreschoolDecisionPage {
  constructor(private readonly page: Page) {}

  readonly status = this.page.findByDataQa('status')
  readonly type = this.page.findByDataQa('type')
  readonly validFrom = this.page.findByDataQa('valid-from')
  readonly extendedCompulsoryEducation = this.page.findByDataQa(
    'extended-compulsory-education'
  )
  readonly grantedServices = this.page.findByDataQa('granted-services')
  readonly grantedServicesBasis = this.page.findByDataQa(
    'granted-services-basis'
  )
  readonly selectedUnit = this.page.findByDataQa('unit')
  readonly primaryGroup = this.page.findByDataQa('primary-group')
  readonly decisionBasis = this.page.findByDataQa('decision-basis')
  readonly documentBasis = this.page.findByDataQa('document-basis')
  readonly basisDocumentsInfo = this.page.findByDataQa('basis-documents-info')
  readonly guardiansHeardOn = this.page.findByDataQa('guardians-heard-on')
  readonly viewOfGuardians = this.page.findByDataQa('view-of-guardians')
  readonly preparer1 = this.page.findByDataQa('preparer-1')
  readonly decisionMaker = this.page.findByDataQa('decision-maker')
}
