// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../../utils/page'

export class VasuTemplateEditPage {
  constructor(readonly page: Page) {}

  saveButton = this.page.findByDataQa('save-template')
}
