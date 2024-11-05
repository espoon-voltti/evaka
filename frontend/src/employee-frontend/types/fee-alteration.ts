// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FeeAlteration } from 'lib-common/generated/api-types/invoicing'

export type PartialFeeAlteration = Omit<
  FeeAlteration,
  'modifiedAt' | 'modifiedBy'
>
