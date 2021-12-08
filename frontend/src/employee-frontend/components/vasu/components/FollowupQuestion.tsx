// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useCallback } from 'react'
import styled, { css } from 'styled-components'
import { v4 as uuid } from 'uuid'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UserContext } from '../../../state/user'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Dimmed, H2, Label } from 'lib-components/typography'
import { Followup, FollowupEntry } from '../vasu-content'
import { QuestionProps } from './question-props'
import QuestionInfo from '../QuestionInfo'
import { VasuTranslations } from 'lib-customizations/employee'
import { defaultMargins } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { blueColors } from 'lib-customizations/common'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faPen } from 'lib-icons'

const FollowupEntryElement = React.memo(function FollowupEntryElement({
  entry,
  onEdited
}: {
  entry: FollowupEntry
  onEdited?: (entry: FollowupEntry) => void
}) {
  const { i18n } = useTranslation()
  const [editing, setEditing] = useState(false)
  const [editedEntry, setEditedEntry] = useState<FollowupEntry>()
  const { user, roles } = useContext(UserContext)

  const onCommitEdited = useCallback(
    (editedEntry: FollowupEntry) => {
      setEditing(false)
      onEdited && onEdited(editedEntry)
      setEditedEntry(editedEntry)
    },
    [onEdited]
  )

  const getEntry = useCallback(
    () => (editedEntry ? editedEntry : entry),
    [entry, editedEntry]
  )

  const editAllowed =
    user?.id === entry.authorId ||
    roles.includes('ADMIN') ||
    roles.includes('UNIT_SUPERVISOR') ||
    roles.includes('STAFF') ||
    roles.includes('SPECIAL_EDUCATION_TEACHER')

  return (
    <Entry>
      {editing && onEdited ? (
        <FollowupEntryEditor
          entry={entry}
          buttonCaption={i18n.common.save}
          onChange={onCommitEdited}
          data-qa="vasu-followup-entry-edit"
        />
      ) : (
        <EntryText data-qa="vasu-followup-entry-text">
          {getEntry().text}
        </EntryText>
      )}
      <FixedSpaceRow>
        <Dimmed data-qa="vasu-followup-entry-metadata">
          {getEntry().date.format()} {getEntry().authorName}
          {getEntry().edited &&
            `, ${i18n.vasu.edited} ${
              getEntry().edited?.editedAt.format() ?? ''
            } ${getEntry().edited?.editorName ?? ''}`}
        </Dimmed>
        {onEdited && editAllowed && (
          <IconButton
            icon={faPen}
            onClick={() => setEditing(true)}
            data-qa="vasu-followup-entry-edit-btn"
          />
        )}
      </FixedSpaceRow>
    </Entry>
  )
})

const Entry = styled.div`
  margin: ${defaultMargins.m} 0px;
`

const EntryText = styled.p`
  white-space: pre-line;
  margin: 0px 0px ${defaultMargins.xxs} 0px;
`

const FollowupEntryEditor = React.memo(function FollowupEntryEditor({
  entry,
  buttonCaption,
  onChange,
  'data-qa': dataQa
}: {
  entry?: FollowupEntry
  buttonCaption: string
  onChange: (entry: FollowupEntry) => void
  'data-qa': string
}) {
  const { user } = useContext(UserContext)
  const [textValue, setTextValue] = useState(entry ? entry.text : '')

  const onSubmit = useCallback(() => {
    if (entry?.id) {
      onChange({
        ...entry,
        text: textValue,
        edited: {
          editedAt: LocalDate.today(),
          editorName: user?.name || 'unknown',
          editorId: user?.id
        }
      })
    } else {
      onChange({
        ...entry,
        id: uuid(),
        date: LocalDate.today(),
        authorName: user?.name || 'unknown',
        authorId: user?.id,
        text: textValue
      })
    }
    setTextValue('')
  }, [onChange, entry, user, textValue])

  return (
    <FollowupEntryInputRow fullWidth>
      <TextArea
        value={textValue}
        onChange={setTextValue}
        data-qa={`${dataQa}-input`}
      />
      <Button
        primary
        type="submit"
        disabled={textValue === ''}
        text={buttonCaption}
        onClick={onSubmit}
        data-qa={`${dataQa}-submit`}
      />
    </FollowupEntryInputRow>
  )
})

const FollowupEntryInputRow = styled(FixedSpaceRow)`
  & > div {
    flex: 1;
  }
`

interface FollowupQuestionProps extends QuestionProps<Followup> {
  onChange?: (value: FollowupEntry) => void
  onEdited?: (value: FollowupEntry) => void
  translations: VasuTranslations
}

export default React.memo(function FollowupQuestion({
  onChange,
  onEdited,
  question: { title, name, value, info },
  translations
}: FollowupQuestionProps) {
  const { i18n } = useTranslation()

  return (
    <FollowupQuestionContainer
      editable={!!onChange}
      data-qa="vasu-followup-question"
    >
      <H2>{title}</H2>
      <QuestionInfo info={info}>
        <Label>{name}</Label>
      </QuestionInfo>
      {value.length > 0 ? (
        value.map((entry, ix) => (
          <FollowupEntryElement key={ix} entry={entry} onEdited={onEdited} />
        ))
      ) : (
        <Dimmed>{translations.noRecord}</Dimmed>
      )}
      {onChange && (
        <FollowupEntryEditor
          buttonCaption={i18n.common.addNew}
          onChange={onChange}
          data-qa="vasu-followup-entry-new"
        />
      )}
    </FollowupQuestionContainer>
  )
})

const FollowupQuestionContainer = styled.div<{ editable: boolean }>`
  ${(p) =>
    p.editable &&
    css`
      border-left: 5px solid ${blueColors.medium};
      box-shadow: 0px -4px 4px rgba(15, 15, 15, 0.1);
      width: calc(100% + 2 * ${defaultMargins.L});
      padding: ${defaultMargins.L};
      position: relative;
      left: -${defaultMargins.L};
    `}
`
