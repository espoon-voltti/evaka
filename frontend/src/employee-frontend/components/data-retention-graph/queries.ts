// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getDataRetentionSchema } from '../../generated/api-clients/dataremoval'

const q = new Queries()

export const dataRetentionSchemaQuery = q.query(getDataRetentionSchema)
