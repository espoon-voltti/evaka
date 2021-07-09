// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Dimmed } from '../../../../lib-components/typography'
import { useTranslation } from '../../../state/i18n'

const PreFormattedText = styled.div`
  white-space: pre-line;
`

interface Props {
  text: string | undefined
}

export function ValueOrNoRecord({ text }: Props): JSX.Element {
  const { i18n } = useTranslation()
  return (
    <PreFormattedText>
      {text || <Dimmed>{i18n.vasu.noRecord}</Dimmed>}
    </PreFormattedText>
  )
}
