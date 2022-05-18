// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Followup, FollowupEntry } from 'lib-common/api-types/vasu'
import { PermittedFollowupActions } from 'lib-common/api-types/vasu'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Dimmed, H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import { useTranslation } from '../../localization'

import { QuestionProps } from './question-props'

const FollowupEntryElement = React.memo(function FollowupEntryElement({
  entry
}: {
  entry: FollowupEntry
}) {
  const i18n = useTranslation()

  return (
    <Entry>
      <EntryText data-qa="vasu-followup-entry-text">{entry.text}</EntryText>
      <FixedSpaceRow>
        <Dimmed data-qa="vasu-followup-entry-metadata">
          {entry.date.format()} {entry.authorName}
          {entry.edited &&
            `, ${i18n.vasu.edited} ${entry.edited?.editedAt.format() ?? ''} ${
              entry.edited?.editorName ?? ''
            }`}
        </Dimmed>
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

interface FollowupQuestionProps extends QuestionProps<Followup> {
  translations: VasuTranslations
  permittedFollowupActions?: PermittedFollowupActions
}

export default React.memo(function FollowupQuestion({
  question: { title, name, value },
  translations
}: FollowupQuestionProps) {
  return (
    <FollowupQuestionContainer data-qa="vasu-followup-question">
      <H2>{title}</H2>
      <Label>{name}</Label>
      {value.length > 0 ? (
        value.map((entry, ix) => (
          <FollowupEntryElement key={ix} entry={entry} />
        ))
      ) : (
        <Dimmed>{translations.noRecord}</Dimmed>
      )}
    </FollowupQuestionContainer>
  )
})

const FollowupQuestionContainer = styled.div``
