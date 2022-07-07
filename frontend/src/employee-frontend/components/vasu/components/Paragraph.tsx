// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Paragraph } from 'lib-common/api-types/vasu'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { H3, Italic, P } from 'lib-components/typography'

interface Props {
  question: Paragraph
  isAtStart: boolean
}

const HorizontalLineWithoutTopMargin = styled(HorizontalLine)`
  margin-top: 0;
`

export default React.memo(function Paragraph({ question, isAtStart }: Props) {
  return (
    <div>
      {question.title && !isAtStart ? (
        <HorizontalLineWithoutTopMargin slim />
      ) : null}
      {question.title ? <H3 noMargin={isAtStart}>{question.title}</H3> : null}
      {question.paragraph ? (
        <P noMargin>
          <Italic>{question.paragraph}</Italic>
        </P>
      ) : null}
    </div>
  )
})
