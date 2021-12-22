// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import colors from 'lib-customizations/common'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { faChild, faComments, faUser } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { useHistory, useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { featureFlags } from 'lib-customizations/employee'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'
import { combine } from 'lib-common/api'
import { defaultMargins } from 'lib-components/white-space'
import { UserContext } from '../../state/user'
import { MessageContext } from '../../state/messages'

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

  background: ${colors.main.primary};
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
  color: ${(p) => (p.selected ? colors.main.lighter : colors.main.light)};
  height: 24px !important;
  width: 24px !important;
  margin: 0;
`

const IconText = styled.span<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.main.lighter : colors.main.light)};
  font-size: 14px;
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
      <FixedSpaceRow justifyContent="center" marginBottom="zero">
        {children}
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="space-evenly" alignItems="center">
        <IconText selected={selected}>{text}</IconText>
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
}

type BottomNavbarProps = {
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
              <CustomIcon icon={faChild} selected={selected === 'child'} />
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
              <CustomIcon icon={faUser} selected={selected === 'staff'} />
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
                  icon={faComments}
                  selected={selected === 'messages'}
                />
                {(user?.pinLoginActive && groupAccountIds.length > 0
                  ? unreadCounts.filter(({ accountId }) =>
                      groupAccountIds.includes(accountId)
                    )
                  : unreadCounts
                ).some(({ unreadCount }) => unreadCount > 0) && (
                  <UnreadMessagesIndicator
                    data-qa={'unread-messages-indicator'}
                  />
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
  background-color: ${colors.accents.warningOrange};
`
