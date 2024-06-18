// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export default class AssistanceNeedPreschoolDecisionPage {
  status: Element
  type: Element
  validFrom: Element
  extendedCompulsoryEducation: Element
  grantedServices: Element
  grantedServicesBasis: Element
  selectedUnit: Element
  primaryGroup: Element
  decisionBasis: Element
  documentBasis: Element
  basisDocumentsInfo: Element
  guardiansHeardOn: Element
  viewOfGuardians: Element
  preparer1: Element
  decisionMaker: Element
  constructor(private readonly page: Page) {
    this.status = page.findByDataQa('status')
    this.type = page.findByDataQa('type')
    this.validFrom = page.findByDataQa('valid-from')
    this.extendedCompulsoryEducation = page.findByDataQa(
      'extended-compulsory-education'
    )
    this.grantedServices = page.findByDataQa('granted-services')
    this.grantedServicesBasis = page.findByDataQa('granted-services-basis')
    this.selectedUnit = page.findByDataQa('unit')
    this.primaryGroup = page.findByDataQa('primary-group')
    this.decisionBasis = page.findByDataQa('decision-basis')
    this.documentBasis = page.findByDataQa('document-basis')
    this.basisDocumentsInfo = page.findByDataQa('basis-documents-info')
    this.guardiansHeardOn = page.findByDataQa('guardians-heard-on')
    this.viewOfGuardians = page.findByDataQa('view-of-guardians')
    this.preparer1 = page.findByDataQa('preparer-1')
    this.decisionMaker = page.findByDataQa('decision-maker')
  }
}
