// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ChildVasuSummary,
  CitizenGetVasuDocumentResponse,
  VasuDocument,
  VasuDocumentEvent
} from 'lib-common/generated/api-types/vasu'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../api-client'

import { mapVasuContent } from './vasu-content'

const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({ ...e, created: new Date(e.created) })

const mapVasuDocumentResponse = ({
  events,
  modifiedAt,
  templateRange,
  basics,
  content,
  ...rest
}: JsonOf<VasuDocument>): VasuDocument => ({
  ...rest,
  content: mapVasuContent(content),
  events: events.map(mapVasuDocumentEvent),
  modifiedAt: new Date(modifiedAt),
  templateRange: FiniteDateRange.parseJson(templateRange),
  basics: {
    ...basics,
    child: {
      ...basics.child,
      dateOfBirth: LocalDate.parseIso(basics.child.dateOfBirth)
    },
    placements:
      basics.placements?.map((pl) => ({
        ...pl,
        range: FiniteDateRange.parseJson(pl.range)
      })) ?? null
  }
})

export async function getGuardianChildVasuSummaries(): Promise<
  Result<ChildVasuSummary[]>
> {
  return client
    .get<JsonOf<ChildVasuSummary[]>>(`/citizen/vasu/children/vasu-summaries`)
    .then((res) =>
      Success.of(
        res.data.map((childVasuSummary) => ({
          ...childVasuSummary,
          vasuDocumentsSummary: childVasuSummary.vasuDocumentsSummary.map(
            ({ events, modifiedAt, publishedAt, ...rest }) => ({
              ...rest,
              events: events.map(mapVasuDocumentEvent),
              modifiedAt: new Date(modifiedAt),
              publishedAt: publishedAt ? new Date(publishedAt) : null
            })
          )
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getCitizenVasuDocument(
  id: UUID
): Promise<Result<CitizenGetVasuDocumentResponse>> {
  return client
    .get<JsonOf<CitizenGetVasuDocumentResponse>>(`/citizen/vasu/${id}`)
    .then((res) =>
      Success.of({
        vasu: mapVasuDocumentResponse(res.data.vasu),
        guardianHasGivenPermissionToShare:
          res.data.guardianHasGivenPermissionToShare
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function givePermissionToShareVasu(
  documentId: UUID
): Promise<Result<void>> {
  try {
    await client.post(`/citizen/vasu/${documentId}/give-permission-to-share`)
    return Success.of()
  } catch (e) {
    return Failure.fromError(e)
  }
}
