// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'

interface Props {
  children: React.ReactNode
  info: string | null
}

const WhitespacePre = styled.div`
  white-space: pre-wrap;
`

export default React.memo(function QuestionInfo({ children, info }: Props) {
  if (info) {
    return (
      <ExpandingInfo info={<WhitespacePre>{info}</WhitespacePre>} width="full">
        {children}
      </ExpandingInfo>
    )
  }

  return <>{children}</>
})
