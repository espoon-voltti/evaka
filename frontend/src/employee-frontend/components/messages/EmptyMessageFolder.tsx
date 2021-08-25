// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faFolderOpen } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { H3 } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import styled from 'styled-components'
import { useTranslation } from 'employee-frontend/state/i18n'
import Loader from 'lib-components/atoms/Loader'

type Props = {
  loading: boolean
}

export default React.memo(function EmptyMessagesFolder({ loading }: Props) {
  const { i18n } = useTranslation()
  return (
    <EmptyThreadViewContainer>
      {loading ? (
        <Loader />
      ) : (
        <>
          <FontAwesomeIcon
            icon={faFolderOpen}
            size={'7x'}
            color={colors.greyscale.medium}
          />
          <H3>{i18n.messages.emptyInbox}</H3>
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
