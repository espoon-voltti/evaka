// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { updatePreferredUiLanguage } from '../generated/api-clients/pis'

const q = new Queries()

export const updatePreferredUiLanguageMutation = q.mutation(
  updatePreferredUiLanguage
)
