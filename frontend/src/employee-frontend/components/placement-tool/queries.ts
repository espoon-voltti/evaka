// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getNextPreschoolTerm } from '../../generated/api-clients/application'

const q = new Queries()

export const nextPreschoolTermQuery = q.query(getNextPreschoolTerm)
