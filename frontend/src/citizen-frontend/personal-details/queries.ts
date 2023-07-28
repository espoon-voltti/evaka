// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation } from 'lib-common/query'

import { updatePersonalData } from './api'

export const updatePersonalDetailsMutation = mutation({
  api: updatePersonalData
})
