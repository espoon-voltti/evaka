// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInbox } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useTranslation } from '../localization'
import { H3 } from 'lib-components/typography'
import colors from 'lib-customizations/common'
import React from 'react'
import styled from 'styled-components'
import { tabletMin } from 'lib-components/breakpoints'

interface Props {
  inboxEmpty: boolean
}

export default React.memo(function EmptyThreadView({ inboxEmpty }: Props) {
  const i18n = useTranslation()
  return inboxEmpty ? (
    <EmptyThreadViewContainer data-qa="inbox-empty">
      <FontAwesomeIcon
        icon={faInbox}
        size={'7x'}
        color={colors.greyscale.medium}
      />
      <H3>{i18n.messages.emptyInbox}</H3>
    </EmptyThreadViewContainer>
  ) : (
    <EmptyThreadViewContainer>
      <H3>{i18n.messages.noSelectedMessage}</H3>
    </EmptyThreadViewContainer>
  )
})

const EmptyThreadViewContainer = styled.div`
  text-align: center;
  width: 100%;
  background: white;
  padding-top: 10%;

  display: none;
  @media (min-width: ${tabletMin}) {
    display: block;
  }
`
