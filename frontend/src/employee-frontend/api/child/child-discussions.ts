// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  ChildDiscussionBody,
  ChildDiscussionWithPermittedActions
} from 'lib-common/generated/api-types/childdiscussion'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getChildDiscussions(
  childId: UUID
): Promise<Result<ChildDiscussionWithPermittedActions[]>> {
  return client
    .get<JsonOf<ChildDiscussionWithPermittedActions[]>>(
      `/child-discussions/${childId}`
    )
    .then((res) => res.data)
    .then((dataArr) =>
      dataArr.map(({ data: discussion, permittedActions }) => ({
        data: {
          ...discussion,
          offeredDate: discussion.offeredDate
            ? LocalDate.parseIso(discussion.offeredDate)
            : null,
          heldDate: discussion.heldDate
            ? LocalDate.parseIso(discussion.heldDate)
            : null,
          counselingDate: discussion.counselingDate
            ? LocalDate.parseIso(discussion.counselingDate)
            : null
        },
        permittedActions
      }))
    )
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function createChildDiscussion(
  childId: UUID,
  body: ChildDiscussionBody
): Promise<Result<void>> {
  return client
    .post(`/child-discussions/${childId}`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function updateChildDiscussion(
  discussionId: UUID,
  body: ChildDiscussionBody
): Promise<Result<void>> {
  return client
    .put(`/child-discussions/${discussionId}`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildDiscussion(
  discussionId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/child-discussions/${discussionId}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
