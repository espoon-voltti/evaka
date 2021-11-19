// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faFolderOpen } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { H3 } from 'lib-components/typography'
import styled from 'styled-components'
import Loader from 'lib-components/atoms/Loader'

type Props = {
  loading: boolean
  iconColor: string
  text: string
}

export default React.memo(function EmptyMessagesFolder({
  loading,
  iconColor,
  text
}: Props) {
  return (
    <EmptyThreadViewContainer>
      {loading ? (
        <Loader />
      ) : (
        <>
          <FontAwesomeIcon icon={faFolderOpen} size={'7x'} color={iconColor} />
          <H3>{text}</H3>
        </>
      )}
    </EmptyThreadViewContainer>
  )
})

const EmptyThreadViewContainer = styled.div`
  text-align: center;
  width: 100%;
  background: white;
  padding-top: 10%;
`
