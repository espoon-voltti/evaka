// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import React from 'react'
import {
  MessageRow,
  Participants,
  ParticipantsAndPreview,
  SentAt,
  Title,
  Truncated,
  TypeAndDate
} from './MessageComponents'
import { MessageTypeChip } from './MessageTypeChip'
import { DraftContent } from './types'

function DraftContent({ draft }: { draft: DraftContent }) {
  return (
    <MessageRow>
      <ParticipantsAndPreview>
        <Participants>{draft.recipientNames?.join(', ') ?? '–'}</Participants>
        <Truncated>
          <Title>{draft.title}</Title>
          {' - '}
          {draft.content}
        </Truncated>
      </ParticipantsAndPreview>
      <TypeAndDate>
        <MessageTypeChip type={draft.type ?? 'MESSAGE'} />
        <SentAt sentAt={draft.created} />
      </TypeAndDate>
    </MessageRow>
  )
}

interface Props {
  drafts: Result<DraftContent[]>
}

export function MessageDrafts({ drafts }: Props) {
  return drafts.mapAll({
    loading() {
      return <Loader />
    },
    failure() {
      return <ErrorSegment />
    },
    success(data) {
      return (
        <>
          {data.map((draft) => (
            <DraftContent key={draft.id} draft={draft} />
          ))}
        </>
      )
    }
  })
}
