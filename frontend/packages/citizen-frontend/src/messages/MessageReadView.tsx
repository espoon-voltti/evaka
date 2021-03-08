// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { tabletMin } from '@evaka/lib-components/src/breakpoints'
import colors from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'
import { H3 } from '@evaka/lib-components/src/typography'
import { formatDate } from '../util'
import { ReceivedBulletin } from '../messages/types'

type Props = {
  bulletin: ReceivedBulletin
}
export default React.memo(function MessageReadView({ bulletin }: Props) {
  return (
    <Container>
      <Header>
        <Title noMargin data-qa="message-reader-title">
          {bulletin.title}
        </Title>
        <FixedSpaceRow spacing="s">
          <span data-qa="message-reader-sender">{bulletin.sender}</span>
          <span>{formatDate(bulletin.sentAt)}</span>
        </FixedSpaceRow>
      </Header>
      {bulletin.content.split('\n').map((text, i) => (
        <div key={i} data-qa="message-reader-content">
          <span>{text}</span>
          <br />
        </div>
      ))}
    </Container>
  )
})

const Container = styled.div`
  width: 100%;
  box-sizing: border-box;
  max-width: 100%;
  min-height: 500px;
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.m};
  min-width: 300px;

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
  }
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  font-weight: 600;
  font-size: 16px;
  margin-bottom: ${defaultMargins.XL};
  flex-wrap: wrap;

  @media (max-width: ${tabletMin}) {
    flex-direction: column;
    flex-wrap: nowrap;
    justify-content: flex-start;
    align-items: flex-start;
  }
`

const Title = styled(H3)`
  font-weight: 600;
  margin-right: ${defaultMargins.XXL};
`
