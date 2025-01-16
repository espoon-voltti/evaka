// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import styled from 'styled-components'

import { FinanceNote } from 'lib-common/generated/api-types/finance'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H4 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'

interface Props {
    open: boolean
    notes: FinanceNote[] //TODO maybe fetch these here?
    // TODO
    //
}

export default React.memo(function PersonFinanceNotesAndMessages({
  open,
  notes
    // TODO
}: Props) {
  const { i18n } = useTranslation()
  const [isOpen, setIsOpen] = useState(open)

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
      {notes.map(note => <FinanceNoteListItem note={note} />)}
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
  // TODO

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
