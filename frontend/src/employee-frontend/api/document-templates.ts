// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { API_URL } from './client'

export function exportDocumentTemplateUrl(id: UUID): string {
  return `${API_URL}/document-templates/${encodeURIComponent(id)}/export`
}
