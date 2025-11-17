// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

const LabelContainer = styled.div`
  margin-bottom: ${defaultMargins.s};
  margin-top: ${defaultMargins.s};
`

const Value = styled.div`
  white-space: pre-line;
`

export default React.memo(function OptionalLabelledValue({
  label,
  value,
  'data-qa': dataQa
}: {
  label: string
  value: React.ReactNode | null | undefined
  'data-qa'?: string
}) {
  if (!value) {
    return null
  }

  return (
    <LabelContainer>
      <Label>{label}</Label>
      <Gap size="xs" />
      <Value data-qa={`labelled-value-${dataQa ?? label}`}>{value}</Value>
    </LabelContainer>
  )
})
