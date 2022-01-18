// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

interface Props {
  text: string
  'data-qa'?: string
}

export default React.memo(function WarningLabel({
  text,
  'data-qa': dataQa
}: Props) {
  return (
    <Wrapper data-qa={dataQa}>
      <FontAwesomeIcon icon={faExclamationTriangle} inverse />
      <Gap size="xs" horizontal />
      {text}
    </Wrapper>
  )
})

const Wrapper = styled.div`
  border-radius: 15px;
  height: 30px;
  padding: 0 10px;
  text-align: center;
  line-height: 30px;
  font-weight: ${fontWeights.semibold};
  background: ${colors.status.danger};
  color: ${colors.grayscale.g0};
`
