// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createCaretakers,
  getCaretakers,
  removeCaretakers,
  updateCaretakers
} from '../../../generated/api-clients/daycare'

const q = new Queries()

export const caretakersQuery = q.query(getCaretakers)

export const createCaretakersMutation = q.mutation(createCaretakers, [
  ({ daycareId, groupId }) => caretakersQuery({ daycareId, groupId })
])

export const updateCaretakersMutation = q.mutation(updateCaretakers, [
  ({ daycareId, groupId }) => caretakersQuery({ daycareId, groupId })
])

export const removeCaretakersMutation = q.mutation(removeCaretakers, [
  ({ daycareId, groupId }) => caretakersQuery({ daycareId, groupId })
])
