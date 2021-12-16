// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Paragraph } from 'lib-common/api-types/vasu'
import { H3, P } from 'lib-components/typography'
import React from 'react'

interface Props {
  question: Paragraph
}

export default React.memo(function Paragraph({ question }: Props) {
  return (
    <div>
      {question.title ? <H3 noMargin>{question.title}</H3> : null}
      {question.paragraph ? <P noMargin>{question.paragraph}</P> : null}
    </div>
  )
})
