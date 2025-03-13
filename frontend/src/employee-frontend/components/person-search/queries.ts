// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { searchPerson } from '../../generated/api-clients/pis'

const q = new Queries()

export const searchPersonQuery = q.query(searchPerson)
