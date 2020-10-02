// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { ApplicationNote } from 'types/application'
import styled from 'styled-components'
import { Greyscale } from 'components/shared/Colors'
import { Label } from 'components/shared/Typography'
import { formatDate } from 'utils/date'
import { formatParagraphs } from 'utils/html-utils'
import { DATE_FORMAT_DATE_TIME } from '~constants'
import { DefaultMargins, Gap } from 'components/shared/layout/white-space'
import { TextArea } from 'components/shared/alpha/elements/form'
import { addSeconds, isAfter } from 'date-fns'
import InlineButton from 'components/shared/atoms/buttons/InlineButton'
import { createNote, deleteNote, updateNote } from 'api/applications'
import { UUID } from 'types'
import { UIContext } from 'state/ui'
import { useTranslation } from 'state/i18n'
import { FixedSpaceRow } from 'components/shared/layout/flex-helpers'
import { faPen, faQuestion, faTrash } from '@evaka/icons'
import IconButton from 'components/shared/atoms/buttons/IconButton'
import InfoModal from '~components/common/InfoModal'

const NoteContainer = styled.div`
  display: flex;
  flex-direction: column;
  padding: ${DefaultMargins.s};
  background: ${Greyscale.white};
`

const TopBar = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin-bottom: ${DefaultMargins.s};
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
  margin-top: ${DefaultMargins.s};
`

const DetailText = styled.span`
  font-size: 12px;
  color: ${Greyscale.medium};
  font-style: italic;
`

interface ReadProps {
  note: ApplicationNote
  editable: boolean
  onStartEdit: () => undefined | void
  onDelete: () => undefined | void
}

interface InputProps {
  onSave: () => undefined | void
  onCancel: () => undefined | void
}

interface EditProps extends InputProps {
  note: ApplicationNote
}

interface CreateProps extends InputProps {
  applicationId: UUID
  onSave: () => undefined | void
  onCancel: () => undefined | void
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
  const [text, setText] = useState<string>('')
  useEffect(() => {
    setText(isEdit(props) ? props.note.text : '')
  }, [props])

  const save = () => {
    if (!isInput(props)) return

    setSubmitting(true)

    void (isEdit(props)
      ? updateNote(props.note.id, text)
      : createNote(props.applicationId, text)
    )
      .then(() => props.onSave())
      .catch(() =>
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.application.notes.error.save
        })
      )
      .finally(() => setSubmitting(false))
  }

  const doDelete = () => {
    if (!isRead(props) || !props.editable) return

    const { note, onDelete } = props

    void deleteNote(note.id)
      .then(() => onDelete())
      .catch(() =>
        setErrorMessage({
          type: 'error',
          title: i18n.common.error.unknown,
          text: i18n.application.notes.error.remove
        })
      )
  }

  const renderDeleteConfirmation = () => (
    <InfoModal
      iconColour={'orange'}
      title={i18n.application.notes.confirmDelete}
      resolveLabel={i18n.common.remove}
      rejectLabel={i18n.common.cancel}
      icon={faQuestion}
      reject={() => setConfirmingDelete(false)}
      resolve={() => doDelete()}
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
                <Label>{props.note.createdByName}</Label>
                <span>
                  {i18n.application.notes.created}:{' '}
                  {formatDate(props.note.created, DATE_FORMAT_DATE_TIME)}
                </span>

                {isEdit(props) ? (
                  <DetailText>{i18n.application.notes.editing}</DetailText>
                ) : isAfter(
                    props.note.updated,
                    addSeconds(props.note.created, 1)
                  ) ? (
                  <>
                    <DetailText>
                      {`${i18n.application.notes.lastEdited}: ${formatDate(
                        props.note.updated,
                        DATE_FORMAT_DATE_TIME
                      )}`}
                    </DetailText>
                    {props.note.updatedByName && (
                      <DetailText>{props.note.updatedByName}</DetailText>
                    )}
                  </>
                ) : null}
              </>
            )}
          </Creator>

          {isRead(props) && props.editable && (
            <FixedSpaceRow spacing="xs">
              <IconButton icon={faPen} onClick={props.onStartEdit} size="s" />
              <IconButton
                icon={faTrash}
                onClick={() => setConfirmingDelete(true)}
                size="s"
                dataQa="delete-note"
              />
            </FixedSpaceRow>
          )}
        </TopBar>

        {isRead(props) && (
          <div data-qa="application-note-content">
            {formatParagraphs(props.note.text)}
          </div>
        )}

        {isInput(props) && (
          <>
            <TextArea
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder={i18n.application.notes.placeholder}
              autoFocus
            />
            <ButtonsBar>
              <InlineButton
                onClick={props.onCancel}
                text={i18n.common.cancel}
                disabled={submitting}
              />
              <Gap horizontal size="s" />
              <InlineButton
                onClick={save}
                text={i18n.common.save}
                disabled={submitting}
                dataQa="save-note"
              />
            </ButtonsBar>
          </>
        )}
      </NoteContainer>
    </>
  )
})
