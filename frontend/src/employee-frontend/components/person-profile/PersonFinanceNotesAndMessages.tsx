// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import styled from 'styled-components'

import { useTranslation } from '../../state/i18n'

interface Props {
    // TODO
    //
}

export default React.memo(function PersonFinanceNotesAndMessages({
    // TODO
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleContentArea
      title={<H2>{i18n.personProfile.financeNotesAndMessages.title}</H2>}
      open={open}
      //toggleOpen
      opaque
      paddingVertical="L"
      data-qa="person-finance-notes-and-messages-collapsible"
    >
      <H4>{i18n.personProfile.financeNotesAndMessages.title}</H4>

    </CollapsibleContentArea>
  )
})

const StyledTextAreaView = styled.textarea`
  // TODO
`

const FinanceNoteView = React.memo(function FinanceNoteView({
  note
}: {
  note: FinanceNote
}) {
  // TODO

  return (
    <>
      <FinanceNoteHeader note={note} />
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

}: {

}) {
  // TODO

  return (

  )
})
