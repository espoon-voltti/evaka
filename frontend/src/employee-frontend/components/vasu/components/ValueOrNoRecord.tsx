// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Dimmed } from 'lib-components/typography'
import type { VasuTranslations } from 'lib-customizations/employee'

const PreFormattedText = styled.div`
  white-space: pre-line;
`

interface Props {
  text: string | undefined
  translations: VasuTranslations
  dataQa?: string | null
}

export function ValueOrNoRecord({
  text,
  translations,
  dataQa = 'value-or-no-record'
}: Props): JSX.Element {
  return (
    <PreFormattedText data-qa={dataQa}>
      {text || <Dimmed>{translations.noRecord}</Dimmed>}
    </PreFormattedText>
  )
}
