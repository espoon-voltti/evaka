// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { combine, Failure, Success } from 'lib-common/api'
import { useQuery } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { ContentArea } from 'lib-components/layout/Container'
import { fontSizesMobile, H1, H2, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import BottomNavBar from '../common/BottomNavbar'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import {
  PushNotifications,
  ServiceWorkerContext
} from '../common/service-worker'
import { UnitContext } from '../common/unit'
import { WideLinkButton } from '../pairing/components'

import { unreadCountsQuery } from './queries'

export const UnreadMessagesPage = React.memo(function UnreadMessagesPage() {
  const { unitId } = useNonNullableParams<{
    unitId: UUID
  }>()
  const { groupRoute } = useSelectedGroup()
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)
  const { user } = useContext(UserContext)
  const { data: unreadCounts = [] } = useQuery(unreadCountsQuery(unitId))
  const { pushNotifications } = useContext(ServiceWorkerContext)

  return renderResult(combine(unitInfoResponse, user), ([unit, user]) => (
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
            to={`${groupRoute}/messages`}
          >
            {i18n.messages.openPinLock}
          </WideLinkButton>
        </ButtonContainer>
      )}
      {pushNotifications && (
        <NotificationSettings pushNotifications={pushNotifications} />
      )}
      <BottomNavBar selected="messages" />
    </ContentArea>
  ))
})

export const HeaderContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`

const NotificationSettings = React.memo(function NotificationSettings({
  pushNotifications
}: {
  pushNotifications: PushNotifications
}) {
  const [state, setState] = useState<PermissionState | undefined>(undefined)

  const { i18n } = useTranslation()
  const refresh = useCallback(() => {
    pushNotifications.permissionState
      .then(setState)
      .catch(() => setState(undefined))
  }, [pushNotifications])

  useEffect(refresh, [refresh])

  const enable = useCallback(
    () =>
      pushNotifications
        .enable()
        .then((success) =>
          success ? Success.of(undefined) : Failure.of({ message: 'denied' })
        )
        .catch((err) => Failure.fromError(err)),
    [pushNotifications]
  )

  return state ? (
    <NotificationSettingsContainer>
      <H2>{i18n.settings.notifications.title}</H2>
      <div>{`${i18n.settings.notifications.label}: ${i18n.settings.notifications.state[state]}`}</div>
      <Gap size="s" />
      {state === 'prompt' && (
        <AsyncButton
          text={i18n.settings.notifications.enable}
          onClick={enable}
          onFailure={refresh}
          onSuccess={refresh}
        />
      )}
    </NotificationSettingsContainer>
  ) : undefined
})

const NotificationSettingsContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
  border-top: 1px solid ${colors.grayscale.g15};
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
