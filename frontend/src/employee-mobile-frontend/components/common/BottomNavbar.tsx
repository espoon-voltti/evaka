// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faChild,
  fasChild,
  faEnvelope,
  fasEnvelope,
  faUser,
  fasUser
} from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { MessageContext } from '../../state/messages'
import { UnitContext } from '../../state/unit'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

export type NavItem = 'child' | 'staff' | 'messages'

const bottomNavBarHeight = 60

const Root = styled.div`
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: ${bottomNavBarHeight}px;

  display: flex;
  justify-content: space-evenly;
  align-items: center;

  background: ${colors.grayscale.g0};
  box-shadow: 0px -4px 10px rgba(0, 0, 0, 0.15);
  margin-bottom: 0 !important;
`

export const ReserveSpace = styled.div`
  height: ${bottomNavBarHeight}px;
`

const Button = styled.div`
  width: 100px;
  position: relative;
`

const CustomIcon = styled(FontAwesomeIcon)<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.main.m1 : colors.grayscale.g70)};
  height: 24px !important;
  width: 24px !important;
  margin: 0;
`

const IconText = styled.span<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.main.m1 : colors.grayscale.g70)};
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
`

type BottomTextProps = {
  text: string
  children: React.ReactNode
  selected: boolean
  onClick: () => void
}

const BottomText = ({ text, children, selected, onClick }: BottomTextProps) => {
  return (
    <FixedSpaceColumn spacing="3px" onClick={onClick}>
      <FixedSpaceRow justifyContent="center">{children}</FixedSpaceRow>
      <FixedSpaceRow justifyContent="space-evenly" alignItems="center">
        <IconText selected={selected}>{text}</IconText>
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
}

export type BottomNavbarProps = {
  selected?: NavItem
}

export default function BottomNavbar({ selected }: BottomNavbarProps) {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { unitId, groupId } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  const { unitInfoResponse, unreadCountsResponse } = useContext(UnitContext)
  const { user } = useContext(UserContext)
  const { groupAccounts } = useContext(MessageContext)

  const groupAccountIds = groupAccounts.map(({ account }) => account.id)

  return renderResult(
    combine(unitInfoResponse, unreadCountsResponse, user),
    ([unit, unreadCounts, user]) => (
      <>
        {/* Reserve navbar's height from the page, so that the fixed navbar doesn't hide anything */}
        <ReserveSpace />
        <Root>
          <Button data-qa="bottomnav-children">
            <BottomText
              text={i18n.common.children}
              selected={selected === 'child'}
              onClick={() =>
                selected !== 'child' &&
                history.push(
                  `/units/${unitId}/groups/${groupId}/child-attendance`
                )
              }
            >
              <CustomIcon
                icon={selected === 'child' ? fasChild : faChild}
                selected={selected === 'child'}
              />
            </BottomText>
          </Button>
          <Button data-qa="bottomnav-staff">
            <BottomText
              text={i18n.common.staff}
              selected={selected === 'staff'}
              onClick={() =>
                selected !== 'staff' &&
                history.push(
                  featureFlags.experimental?.realtimeStaffAttendance
                    ? `/units/${unitId}/groups/${groupId}/staff-attendance`
                    : `/units/${unitId}/groups/${groupId}/staff`
                )
              }
            >
              <CustomIcon
                icon={selected === 'staff' ? fasUser : faUser}
                selected={selected === 'staff'}
              />
            </BottomText>
          </Button>
          {unit.features.includes('MOBILE_MESSAGING') ? (
            <Button data-qa="bottomnav-messages">
              <BottomText
                text={i18n.common.messages}
                selected={selected === 'messages'}
                onClick={() =>
                  selected !== 'messages' &&
                  history.push(
                    user?.pinLoginActive
                      ? `/units/${unitId}/groups/${unit.groups[0].id}/messages`
                      : `/units/${unitId}/groups/${unit.groups[0].id}/messages/unread-messages`
                  )
                }
              >
                <CustomIcon
                  icon={selected === 'messages' ? fasEnvelope : faEnvelope}
                  selected={selected === 'messages'}
                />
                {(user?.pinLoginActive && groupAccountIds.length > 0
                  ? unreadCounts.filter(({ accountId }) =>
                      groupAccountIds.includes(accountId)
                    )
                  : unreadCounts
                ).some(({ unreadCount }) => unreadCount > 0) && (
                  <UnreadMessagesIndicator data-qa="unread-messages-indicator" />
                )}
              </BottomText>
            </Button>
          ) : null}
        </Root>
      </>
    )
  )
}

const UnreadMessagesIndicator = styled.div`
  position: absolute;
  top: 0;
  right: 25%;
  width: ${defaultMargins.s};
  height: ${defaultMargins.s};
  border-radius: 100%;
  background-color: ${colors.status.warning};
`
