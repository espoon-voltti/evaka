// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import colors from 'lib-customizations/common'
import { Gap } from 'lib-components/white-space'

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
  font-weight: 600;
  background: ${colors.accents.red};
  color: ${colors.greyscale.white};
`
