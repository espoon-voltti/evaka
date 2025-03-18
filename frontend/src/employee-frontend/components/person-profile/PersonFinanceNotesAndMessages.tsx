// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { FinanceNoteId, PersonId } from 'lib-common/generated/api-types/shared'
import { cancelMutation, useQueryResult } from 'lib-common/query'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  CollapsibleContentArea,
  ContentArea
} from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label, Light } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatParagraphs } from '../../utils/html-utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import {
  createFinanceNoteMutation,
  deleteFinanceNoteMutation,
  financeNotesQuery,
  updateFinanceNoteMutation
} from './queries'
import { PersonContext } from './state'

interface Props {
  id: PersonId
  open: boolean
}

export default React.memo(function PersonFinanceNotesAndMessages({
  id,
  open: startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(PersonContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)
  const [open, setIsOpen] = useState(startOpen)
  const financeNotes = useQueryResult(financeNotesQuery({ personId: id }))
  const [text, setText] = useState<string>('')
  const [confirmDelete, setConfirmDelete] = useState<FinanceNoteId>()

  return (
    <>
      {uiMode.startsWith('delete-finance-note') && (
        <MutateFormModal
          type="warning"
          title={i18n.personProfile.financeNotesAndMessages.confirmDelete}
          icon={faQuestion}
          resolveMutation={deleteFinanceNoteMutation}
          resolveAction={() =>
            confirmDelete !== undefined
              ? {
                  id,
                  noteId: confirmDelete
                }
              : cancelMutation
          }
          resolveLabel={i18n.common.remove}
          resolveDisabled={false}
          rejectAction={() => {
            setConfirmDelete(undefined)
            clearUiMode()
          }}
          rejectLabel={i18n.common.cancel}
          onSuccess={() => {
            setConfirmDelete(undefined)
            clearUiMode()
          }}
          data-qa="delete-finance-note-modal"
        />
      )}

      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.financeNotesAndMessages.title}</H2>}
        open={open}
        toggleOpen={() => setIsOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-finance-notes-and-messages-collapsible"
      >
        <BorderedContentArea
          opaque
          paddingHorizontal="0"
          paddingVertical="s"
          data-qa="add-finance-note"
        >
          <FlexRow>
            <AddButton
              text={i18n.personProfile.financeNotesAndMessages.addNote}
              onClick={() => {
                setText('')
                toggleUiMode('add-finance-note')
              }}
              data-qa="add-finance-note-button"
              disabled={
                !permittedActions.has('CREATE_FINANCE_NOTE') ||
                uiMode === 'add-finance-note' ||
                uiMode.startsWith('edit-finance-note')
              }
            />
          </FlexRow>
        </BorderedContentArea>

        {uiMode === 'add-finance-note' && (
          <BorderedContentArea
            opaque
            paddingHorizontal="0"
            paddingVertical="s"
            data-qa="add-finance-note"
          >
            <StyledTextArea
              autoFocus
              value={text}
              rows={3}
              onChange={setText}
              data-qa="finance-note-text-area"
            />
            <Gap size="xs" />
            <FixedSpaceRow justifyContent="flex-start">
              <Button
                appearance="inline"
                onClick={() => clearUiMode()}
                text={i18n.common.cancel}
              />
              <Gap horizontal size="s" />
              <MutateButton
                data-qa="save-finance-note"
                appearance="inline"
                text={i18n.common.save}
                mutation={createFinanceNoteMutation}
                onClick={() => ({ body: { content: text, personId: id } })}
                onSuccess={() => clearUiMode()}
                disabled={!text}
              />
            </FixedSpaceRow>
          </BorderedContentArea>
        )}

        {renderResult(financeNotes, (notes) => (
          // list note items eiter edit or view mode
          <>
            {notes.length > 0
              ? notes.map(({ note, permittedActions }) => (
                  <BorderedContentArea
                    key={note.id}
                    opaque
                    paddingHorizontal="0"
                    paddingVertical="s"
                    data-qa="add-finance-note"
                  >
                    <FlexRow justifyContent="space-between">
                      <FixedSpaceColumn spacing="xxs">
                        <Label>
                          {i18n.personProfile.financeNotesAndMessages.note}
                        </Label>
                        <Light style={{ fontSize: '14px' }}>
                          {i18n.personProfile.financeNotesAndMessages.created}{' '}
                          <span data-qa="finance-note-created-at">
                            {note.createdAt.format()}
                          </span>
                          ,{note.createdByName}
                        </Light>
                        {uiMode === `edit-finance-note_${note.id}` && (
                          <Light style={{ fontSize: '14px' }}>
                            {i18n.personProfile.financeNotesAndMessages.inEdit}
                          </Light>
                        )}
                      </FixedSpaceColumn>

                      {uiMode !== `edit-finance-note_${note.id}` && (
                        <FixedSpaceRow spacing="xs">
                          <IconOnlyButton
                            icon={faPen}
                            onClick={() => {
                              setText(note.content)
                              toggleUiMode(`edit-finance-note_${note.id}`)
                            }}
                            disabled={!permittedActions.includes('UPDATE')}
                            size="s"
                            data-qa="edit-finance-note"
                            aria-label={i18n.common.edit}
                          />
                          <IconOnlyButton
                            icon={faTrash}
                            onClick={() => {
                              setConfirmDelete(note.id)
                              toggleUiMode(`delete-finance-note-${note.id}`)
                            }}
                            disabled={!permittedActions.includes('DELETE')}
                            size="s"
                            data-qa="delete-finance-note"
                            aria-label={i18n.common.remove}
                          />
                        </FixedSpaceRow>
                      )}
                    </FlexRow>
                    <Gap size="xs" />

                    {uiMode === `edit-finance-note_${note.id}` ? (
                      <div key={note.id} data-qa="edit-finance-note">
                        <StyledTextArea
                          autoFocus
                          value={text}
                          rows={3}
                          onChange={setText}
                          data-qa="finance-note-text-area"
                        />
                        <Gap size="xs" />
                        <FixedSpaceRow justifyContent="flex-start">
                          <Button
                            appearance="inline"
                            onClick={() => clearUiMode()}
                            text={i18n.common.cancel}
                          />
                          <MutateButton
                            data-qa="update-finance-note"
                            appearance="inline"
                            text={i18n.common.save}
                            mutation={updateFinanceNoteMutation}
                            onClick={() => ({
                              id,
                              noteId: note.id,
                              body: { content: text, personId: id }
                            })}
                            onSuccess={() => clearUiMode()}
                            disabled={!text}
                          />
                        </FixedSpaceRow>
                      </div>
                    ) : (
                      <div>{formatParagraphs(note.content)}</div>
                    )}
                  </BorderedContentArea>
                ))
              : null}
          </>
        ))}
      </CollapsibleContentArea>
    </>
  )
})

const BorderedContentArea = styled(ContentArea)`
  border-bottom: solid 1px ${(p) => p.theme.colors.grayscale.g15};
`
const StyledTextArea = styled(TextArea)`
  width: 100%;
  max-width: 851px;
  resize: none;
  flex-grow: 1;
  min-height: 100px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
`
