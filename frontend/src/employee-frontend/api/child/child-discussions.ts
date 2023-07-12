// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildDiscussion,
  ChildDiscussionBody
} from 'lib-common/generated/api-types/childdiscussion'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getChildDiscussion(
  childId: UUID
): Promise<ChildDiscussion> {
  return client
    .get<JsonOf<ChildDiscussion>>(`/child-discussions/${childId}`)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      offeredDate: data.offeredDate
        ? LocalDate.parseIso(data.offeredDate)
        : null,
      heldDate: data.heldDate ? LocalDate.parseIso(data.heldDate) : null,
      counselingDate: data.counselingDate
        ? LocalDate.parseIso(data.counselingDate)
        : null
    }))
}

export async function createChildDiscussion(
  childId: UUID,
  body: ChildDiscussionBody
): Promise<void> {
  return client.post(`/child-discussions/${childId}`, body)
}

export async function updateChildDiscussion(
  childId: UUID,
  body: ChildDiscussionBody
): Promise<void> {
  return client.put(`/child-discussions/${childId}`, body)
}
