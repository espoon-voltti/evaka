// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import React, { useContext } from 'react'
import {
  MessageRow,
  Participants,
  ParticipantsAndPreview,
  SentAt,
  Title,
  Truncated,
  TypeAndDate
} from './MessageComponents'
import { MessagesPageContext } from './MessagesPageContext'
import { MessageTypeChip } from './MessageTypeChip'
import { DraftContent } from './types'

interface RowProps {
  draft: DraftContent
}

function DraftRow({ draft }: RowProps) {
  const { setSelectedDraft } = useContext(MessagesPageContext)
  return (
    <MessageRow
      onClick={() => setSelectedDraft(draft)}
      data-qa="draft-message-row"
    >
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
            <DraftRow key={draft.id} draft={draft} />
          ))}
        </>
      )
    }
  })
}
