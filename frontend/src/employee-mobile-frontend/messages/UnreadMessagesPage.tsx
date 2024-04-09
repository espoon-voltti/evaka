// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useQuery, useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import { fontSizesMobile, H1, P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import BottomNavBar from '../common/BottomNavbar'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup, toUnitOrGroup } from '../common/unit-or-group'
import { WideLinkButton } from '../pairing/components'
import { unitInfoQuery } from '../units/queries'

import { unreadCountsQuery } from './queries'

export const UnreadMessagesPage = React.memo(function UnreadMessagesPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { i18n } = useTranslation()
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const { user } = useContext(UserContext)
  const { data: unreadCounts = [] } = useQuery(unreadCountsQuery(unitId))

  return renderResult(combine(unitInfoResponse, user), ([unit, user]) => (
    <ContentArea
      opaque
      fullHeight
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <TopBar title={unit.name} unitId={unitId} />
      <HeaderContainer>
        <H1 noMargin={true}>{i18n.messages.unreadMessages}</H1>
      </HeaderContainer>
      <UnreadCounts>
        {unit.groups.map((group) => (
          <UnreadCountByGroupRow key={group.id}>
            <LinkToGroupMessages
              data-qa={`link-to-group-messages-${group.id}`}
              to={
                routes.messages(toUnitOrGroup({ unitId, groupId: group.id }))
                  .value
              }
            >
              {group.name}
            </LinkToGroupMessages>
            <UnreadCountNumber
              dataQa={`unread-count-by-group-${group.id}`}
              maybeNumber={
                unreadCounts.find((c) => c.groupId === group.id)?.unreadCount
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
            to={routes.messages(unitOrGroup).value}
          >
            {i18n.messages.openPinLock}
          </WideLinkButton>
        </ButtonContainer>
      )}
      <BottomNavBar selected="messages" unitOrGroup={unitOrGroup} />
    </ContentArea>
  ))
})

export const HeaderContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`

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
  color: ${colors.main.m1};
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
  border: 1px solid ${colors.grayscale.g100};
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
