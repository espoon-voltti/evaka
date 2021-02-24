// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import { H3 } from '@evaka/lib-components/src/typography'
import { formatDate } from '~util'
import { ReceivedBulletin } from '~messages/types'
import { messagesBreakpoint } from '~messages/const'

type Props = {
  bulletin: ReceivedBulletin
}
export default React.memo(function MessageReadView({ bulletin }: Props) {
  return (
    <Container>
      <Header>
        <Title>{bulletin.title}</Title>
        <FixedSpaceRow spacing="s">
          <span>{bulletin.sender}</span>
          <span>{formatDate(bulletin.sentAt)}</span>
        </FixedSpaceRow>
      </Header>
      {bulletin.content.split('\n').map((text, i) => (
        <React.Fragment key={i}>
          <span>{text}</span>
          <br />
        </React.Fragment>
      ))}
    </Container>
  )
})

const Container = styled.div`
  flex-grow: 1;
  box-sizing: border-box;
  max-width: 100%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.m};

  @media (max-width: ${messagesBreakpoint}) {
    padding: ${defaultMargins.s};
  }
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-weight: 600;
  font-size: 16px;
  margin-bottom: ${defaultMargins.L};

  @media (max-width: ${messagesBreakpoint}) {
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;
  }
`

const Title = styled(H3)`
  font-weight: 600;
`
