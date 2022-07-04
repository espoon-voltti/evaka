// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Followup } from 'lib-common/api-types/vasu'
import { Dimmed, H2, Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import { QuestionProps } from './question-props'

interface FollowupQuestionProps extends QuestionProps<Followup> {
  translations: VasuTranslations
}

export default React.memo(function FollowupQuestion({
  question: { title, name, value, continuesNumbering },
  translations,
  questionNumber
}: FollowupQuestionProps) {
  return (
    <FollowupQuestionContainer data-qa="vasu-followup-question">
      <H2>{title}</H2>
      <Label>{`${
        continuesNumbering ? `${questionNumber} ` : ''
      }${name}`}</Label>

      <div>
        {value.length > 0 ? (
          <ul>
            {value.map((entry, ix) => (
              <li key={ix}>
                {entry.date.format()}: {entry.text}
              </li>
            ))}
          </ul>
        ) : (
          <Dimmed>{translations.noRecord}</Dimmed>
        )}
      </div>
    </FollowupQuestionContainer>
  )
})

const FollowupQuestionContainer = styled.div``
