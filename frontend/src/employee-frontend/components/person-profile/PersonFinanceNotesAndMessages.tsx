// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { FinanceNote } from 'lib-common/generated/api-types/finance'
import { EvakaUserId } from 'lib-common/generated/api-types/shared'
import { useApiState } from 'lib-common/utils/useRestApi'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H4 } from 'lib-components/typography'

import { getNotes } from '../../generated/api-clients/finance'
import { useTranslation } from '../../state/i18n'

import { renderResult } from '../async-rendering'

const getNotesResult = wrapResult(getNotes)

interface Props {
    open: boolean
    id: EvakaUserId
    // TODO
    //
}

export default React.memo(function PersonFinanceNotesAndMessages({
  open,
  id
    // TODO
}: Props) {
  const { i18n } = useTranslation()
  const [isOpen, setIsOpen] = useState(open)

  // TODO tämä ei ihan toimi vielä.
  const notes = useApiState(
    () => getNotesResult({ adultId: id }),
    [id]
  )

  // TODO nootteja tulee hakea silloin, kun collapsible avataan.

  return (
    <CollapsibleContentArea
      title={<H2>{i18n.personProfile.financeNotesAndMessages.title}</H2>}
      open={open}
      toggleOpen={() => setIsOpen(!isOpen)}
      opaque
      paddingVertical="L"
      data-qa="person-finance-notes-and-messages-collapsible"
    >
      <H4>{i18n.personProfile.financeNotesAndMessages.title}</H4>
      {renderResult(notes, (notes) => notes.map(note => <FinanceNoteListItem note={note} />))}
    </CollapsibleContentArea>
  )
})

const FinanceNoteListItem = React.memo(function FinanceNoteListItem({
  note
}: {
  note: FinanceNote
}) {
  const [isEditing, setIsEditing] = useState(false)

  return isEditing ? (
    <FinanceNoteView note={note} edit={() => setIsEditing(true)} />
  ) : <FinanceNoteEdit note={note} />
})

const StyledTextAreaView = styled.textarea`
  // TODO
`

const FinanceNoteView = React.memo(function FinanceNoteView({
  note,
  edit
}: {
  note: FinanceNote
  edit: () => void
}) {
  // TODO

  return (
    <>
      <FinanceNoteHeader note={note} edit={edit} />
      <StyledTextAreaView>{note.content}</StyledTextAreaView>
    </>
  )
})

const StyledTextAreaEdit = styled.textarea`
  // TODO
`

const CancelSaveButtonContainer = styled.div`
  // TODO
`

const FinanceNoteEdit = React.memo(function FinanceNoteEdit({
  note
} : {
  note: FinanceNote
}) {
  const [content, setContent] = useState(note.content)
  // TODO Päivitä nootti kun oikea nappain painetaan

  return (
    <>
      <FinanceNoteHeader note={note} />
      <StyledTextAreaEdit
        onChange={e => setContent(e.target.value)}
      >
        {content}
      </StyledTextAreaEdit>
      <CancelSaveButtonContainer>
        <button>Accept</button>
        <button>Cancel</button>
        // TODO buttons
      </CancelSaveButtonContainer>
    </>
  )
})

const FinanceNoteHeader = React.memo(function FinanceNoteHeader({
  note,
  edit
}: {
  note: FinanceNote
  edit?: () => void
}) {
  // TODO

  return (
    "TODO: FinanceNoteHeader"
  )
})
