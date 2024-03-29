// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faFolderOpen } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import Loader from 'lib-components/atoms/Loader'
import { H3 } from 'lib-components/typography'

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
          <FontAwesomeIcon icon={faFolderOpen} size="7x" color={iconColor} />
          <H3 data-qa="empty-inbox-text">{text}</H3>
        </>
      )}
    </EmptyThreadViewContainer>
  )
})

const EmptyThreadViewContainer = styled.div`
  text-align: center;
  width: 100%;
  background: ${(p) => p.theme.colors.grayscale.g0};
  padding-top: 10%;
`
