// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createPerson,
  getOrCreatePersonBySsn
} from '../../generated/api-clients/pis'

const q = new Queries()

export const getOrCreatePersonBySsnMutation = q.mutation(getOrCreatePersonBySsn)
export const createPersonMutation = q.mutation(createPerson)
