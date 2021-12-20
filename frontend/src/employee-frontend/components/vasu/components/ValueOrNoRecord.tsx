// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Dimmed } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

const PreFormattedText = styled.div`
  white-space: pre-line;
`

interface Props {
  text: string | undefined
  translations: VasuTranslations
}

export function ValueOrNoRecord({ text, translations }: Props): JSX.Element {
  return (
    <PreFormattedText data-qa="value-or-no-record">
      {text || <Dimmed>{translations.noRecord}</Dimmed>}
    </PreFormattedText>
  )
}
