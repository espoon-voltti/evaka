// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useCallback } from 'react'
import styled, { css } from 'styled-components'
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

const FollowupEntryElement = React.memo(function FollowupEntryElement({
  entry
}: {
  entry: FollowupEntry
}) {
  return (
    <Entry>
      <EntryText data-qa="vasu-followup-entry-text">{entry.text}</EntryText>
      <Dimmed data-qa="vasu-followup-entry-metadata">
        {entry.date.format()} {entry.authorName}
      </Dimmed>
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
  onChange
}: {
  entry?: FollowupEntry
  onChange: (entry: FollowupEntry) => void
}) {
  const { user } = useContext(UserContext)
  const { i18n } = useTranslation()
  const [textValue, setTextValue] = useState(entry ? entry.text : '')

  const onSubmit = useCallback(() => {
    onChange({
      date: LocalDate.today(),
      authorName: user?.name || 'unknown',
      text: textValue
    })
    setTextValue('')
  }, [onChange, user, textValue])

  return (
    <FollowupEntryInputRow fullWidth>
      <TextArea
        value={textValue}
        onChange={setTextValue}
        data-qa="vasu-followup-input"
      />
      <Button
        primary
        type="submit"
        disabled={textValue === ''}
        text={i18n.common.addNew}
        onClick={onSubmit}
        data-qa="vasu-followup-addBtn"
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
  translations: VasuTranslations
}

export default React.memo(function FollowupQuestion({
  onChange,
  question: { title, name, value, info },
  translations
}: FollowupQuestionProps) {
  const addNewEntry = useCallback(
    (entry: FollowupEntry) => {
      onChange && onChange(entry)
    },
    [onChange]
  )

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
          <FollowupEntryElement key={ix} entry={entry} />
        ))
      ) : (
        <Dimmed>{translations.noRecord}</Dimmed>
      )}
      {onChange && <FollowupEntryEditor onChange={addNewEntry} />}
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
