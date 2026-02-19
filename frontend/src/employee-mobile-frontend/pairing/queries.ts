// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getPairingStatus,
  postPairingChallenge
} from '../generated/api-clients/pairing'

const q = new Queries()

export const postPairingChallengeMutation = q.mutation(postPairingChallenge)
export const getPairingStatusMutation = q.mutation(getPairingStatus)
