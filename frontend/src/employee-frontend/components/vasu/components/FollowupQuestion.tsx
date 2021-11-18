// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'
import TextArea from 'lib-components/atoms/form/TextArea'
import { H2, Label } from 'lib-components/typography'
import { Followup } from '../vasu-content'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'
import QuestionInfo from '../QuestionInfo'
import { VasuTranslations } from 'lib-customizations/employee'
import { defaultMargins } from 'lib-components/white-space'
import { blueColors } from 'lib-customizations/common'

interface FollowupQuestionProps extends QuestionProps<Followup> {
  onChange?: (value: string) => void
  translations: VasuTranslations
}

export default React.memo(function FollowupQuestion({
  onChange,
  question: { title, name, info },
  translations
}: FollowupQuestionProps) {
  const getEditorOrStaticText = () => {
    if (!onChange) {
      return <ValueOrNoRecord text={''} translations={translations} />
    }
    return <TextArea value={''} onChange={onChange} />
  }

  return (
    <FollowupQuestionContainer editable={!!onChange}>
      <H2>{title}</H2>
      <QuestionInfo info={info}>
        <Label>{name}</Label>
      </QuestionInfo>
      {getEditorOrStaticText()}
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
