// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeRequest } from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'

// eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
export type IncomeId = 'new' | UUID

export type IncomeFields = IncomeRequest['data']
