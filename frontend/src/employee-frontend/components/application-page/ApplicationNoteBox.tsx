// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type { ApplicationNote } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useMutation } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { faEnvelope, faPen, faQuestion, faTrash } from 'lib-icons'

import {
  createApplicationNoteMutation,
  deleteApplicationNoteMutation,
  updateApplicationNoteMutation
} from '../../components/application-page/queries'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatParagraphs } from '../../utils/html-utils'

const NoteContainer = styled.div`
  display: flex;
  flex-direction: column;
  padding: ${defaultMargins.s};
  background: ${colors.grayscale.g0};
`

const TopBar = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.s};
`

const Creator = styled.div`
  display: flex;
  flex-direction: column;
  font-size: 12px;
`

const ButtonsBar = styled.div`
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: ${defaultMargins.s};
`

const DetailText = styled(Light)`
  font-size: 12px;
`

const NoteIcon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xxs};
`

interface ReadProps {
  note: ApplicationNote
  editable: boolean
  deletable: boolean
  onStartEdit: () => void
  onDelete?: () => void
}

interface InputProps {
  onSave: () => void
  onCancel: () => void
}

interface EditProps extends InputProps {
  note: ApplicationNote
}

interface CreateProps extends InputProps {
  applicationId: ApplicationId
  onSave: () => void
  onCancel: () => void
}

type Props = ReadProps | EditProps | CreateProps

function isInput(props: Props): props is EditProps | CreateProps {
  return (props as InputProps).onSave !== undefined
}

function isEdit(props: Props): props is EditProps {
  return isInput(props) && (props as EditProps).note !== undefined
}

function isCreate(props: Props): props is CreateProps {
  return isInput(props) && (props as CreateProps).applicationId !== undefined
}

function isRead(props: Props): props is ReadProps {
  return !isInput(props)
}

export default React.memo(function ApplicationNoteBox(props: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)

  const [submitting, setSubmitting] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [text, setText] = useState<string>(
    isCreate(props) ? '' : props.note.content
  )

  const { mutateAsync: updateNote } = useMutation(updateApplicationNoteMutation)
  const { mutateAsync: createNote } = useMutation(createApplicationNoteMutation)
  const { mutateAsync: deleteNote } = useMutation(deleteApplicationNoteMutation)

  const save = () => {
    if (!isInput(props)) return

    setSubmitting(true)

    void (
      isEdit(props)
        ? updateNote({
            applicationId: props.note.applicationId,
            noteId: props.note.id,
            body: { text }
          })
        : createNote({ applicationId: props.applicationId, body: { text } })
    )
      .then(() => {
        props.onSave()
        setSubmitting(false)
      })
      .catch(() => {
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.application.notes.error.save,
          resolveLabel: i18n.common.ok
        })
        setSubmitting(false)
      })
  }

  const doDelete = () => {
    if (!isRead(props) || !props.editable) return

    const { note, onDelete } = props

    void deleteNote({ applicationId: note.applicationId, noteId: note.id })
      .then(() => onDelete && onDelete())
      .catch(() =>
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.application.notes.error.remove,
          resolveLabel: i18n.common.ok
        })
      )
  }

  const renderDeleteConfirmation = () => (
    <InfoModal
      type="warning"
      title={i18n.application.notes.confirmDelete}
      icon={faQuestion}
      reject={{
        action: () => setConfirmingDelete(false),
        label: i18n.common.cancel
      }}
      resolve={{
        action: () => doDelete(),
        label: i18n.common.remove
      }}
    />
  )

  return (
    <>
      {confirmingDelete && renderDeleteConfirmation()}

      <NoteContainer data-qa="note-container">
        <TopBar>
          <Creator>
            {isCreate(props) ? (
              <Label>{i18n.application.notes.newNote}</Label>
            ) : (
              <>
                <Label>
                  <>
                    {props.note.messageThreadId !== null && (
                      <NoteIcon icon={faEnvelope} />
                    )}
                    {props.note.createdByName}
                  </>
                </Label>
                <span>
                  {i18n.application.notes.created}:{' '}
                  {props.note.created.format()}
                </span>

                {isEdit(props) ? (
                  <DetailText>{i18n.application.notes.editing}</DetailText>
                ) : props.note.updated.isAfter(
                    props.note.created.addSeconds(1)
                  ) ? (
                  <>
                    <DetailText>
                      {`${
                        i18n.application.notes.lastEdited
                      }: ${props.note.updated.format()}`}
                    </DetailText>
                    <DetailText>{props.note.updatedByName}</DetailText>
                  </>
                ) : null}
              </>
            )}
          </Creator>

          {isRead(props) && (props.editable || props.deletable) && (
            <FixedSpaceRow spacing="xs">
              {props.editable && (
                <IconOnlyButton
                  icon={faPen}
                  onClick={() => {
                    props.onStartEdit()
                    setText(props.note.content)
                  }}
                  size="s"
                  data-qa="edit-note"
                  aria-label={i18n.common.edit}
                />
              )}
              {props.deletable && (
                <IconOnlyButton
                  icon={faTrash}
                  onClick={() => setConfirmingDelete(true)}
                  size="s"
                  data-qa="delete-note"
                  aria-label={i18n.common.remove}
                />
              )}
            </FixedSpaceRow>
          )}
        </TopBar>

        {isRead(props) && (
          <div data-qa="application-note-content">
            {props.note.messageThreadId !== null && (
              <span>
                {i18n.application.notes.sent}{' '}
                <Link
                  data-qa="note-message-thread-link"
                  to={`/messages?applicationId=${props.note.applicationId}&messageBox=thread&threadId=${props.note.messageThreadId}`}
                  target="_blank"
                >
                  {i18n.application.notes.message}
                </Link>
              </span>
            )}
            {formatParagraphs(props.note.content)}
          </div>
        )}

        {isInput(props) && (
          <>
            <TextArea
              value={text}
              onChange={setText}
              placeholder={i18n.application.notes.placeholder}
              autoFocus
            />
            <ButtonsBar>
              <Button
                appearance="inline"
                onClick={props.onCancel}
                text={i18n.common.cancel}
                disabled={submitting}
              />
              <Gap horizontal size="s" />
              <Button
                appearance="inline"
                onClick={save}
                text={i18n.common.save}
                disabled={submitting}
                data-qa="save-note"
              />
            </ButtonsBar>
          </>
        )}
      </NoteContainer>
    </>
  )
})
