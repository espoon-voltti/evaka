// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Link } from 'react-router-dom'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { combine } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { fontSizesMobile, H1, P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import BottomNavBar from '../common/BottomNavbar'
import TopBar from '../common/TopBar'
import { WideLinkButton } from '../mobile/components'
import { HeaderContainer } from './MessagesPage'

export const UnreadMessagesPage = React.memo(function UnreadMessagesPage() {
  const { unitId, groupId } = useParams<{
    unitId: UUID
    groupId: UUID
  }>()
  const { i18n } = useTranslation()
  const { unitInfoResponse, unreadCountsResponse } = useContext(UnitContext)
  const { user } = useContext(UserContext)

  return renderResult(
    combine(unitInfoResponse, unreadCountsResponse, user),
    ([unit, counts, user]) => (
      <ContentArea
        opaque
        fullHeight
        paddingHorizontal="zero"
        paddingVertical="zero"
      >
        <TopBar title={unit.name} />
        <HeaderContainer>
          <H1 noMargin={true}>{i18n.messages.unreadMessages}</H1>
        </HeaderContainer>
        <UnreadCounts>
          {unit.groups.map((group) => (
            <UnreadCountByGroupRow key={group.id}>
              <LinkToGroupMessages
                data-qa={`link-to-group-messages-${group.id}`}
                to={`/units/${unitId}/groups/${group.id}/messages`}
              >
                {group.name}
              </LinkToGroupMessages>
              <UnreadCountNumber
                dataQa={`unread-count-by-group-${group.id}`}
                maybeNumber={
                  counts.find((c) => c.groupId === group.id)?.unreadCount
                }
              />
            </UnreadCountByGroupRow>
          ))}
        </UnreadCounts>
        {!user?.pinLoginActive && (
          <ButtonContainer>
            <P noMargin={true} centered={true}>
              {i18n.messages.pinLockInfo}
            </P>
            <WideLinkButton
              $primary
              data-qa="pin-login-button"
              to={`/units/${unitId}/groups/${groupId}/messages`}
            >
              {i18n.messages.openPinLock}
            </WideLinkButton>
          </ButtonContainer>
        )}
        <BottomNavBar selected="messages" />
      </ContentArea>
    )
  )
})

const UnreadCountNumber = React.memo(function UnreadCountNumber({
  maybeNumber,
  dataQa
}: {
  maybeNumber: number | undefined
  dataQa: string
}) {
  return maybeNumber && maybeNumber > 0 ? (
    <UnreadCountNumberCircle data-qa={dataQa}>
      {maybeNumber}
    </UnreadCountNumberCircle>
  ) : (
    <AllReadIndicator data-qa={dataQa}>&ndash;</AllReadIndicator>
  )
})

const LinkToGroupMessages = styled(Link)`
  font-size: ${fontSizesMobile.h2};
  font-weight: bold;
  color: ${colors.main.dark};
  margin: ${defaultMargins.xs} 0;
`

const UnreadCounts = styled.div`
  display: flex;
  flex-direction: column;
  padding: ${defaultMargins.s};
`

const UnreadCountByGroupRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const UnreadCountNumberCircle = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  width: ${defaultMargins.m};
  height: ${defaultMargins.m};
  border: 1px solid ${colors.greyscale.darkest};
  border-radius: 100%;
`

const AllReadIndicator = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  width: ${defaultMargins.m};
  height: ${defaultMargins.m};
`

const ButtonContainer = styled.div`
  position: sticky;
  bottom: 60px;
  padding: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xs};
`
