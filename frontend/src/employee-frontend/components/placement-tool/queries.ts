// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getNextPreschoolTerm } from 'employee-frontend/generated/api-clients/application'
import { Queries } from 'lib-common/query'

const q = new Queries()

export const nextPreschoolTermQuery = q.query(getNextPreschoolTerm)
