// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasGear } from 'Icons'
import React, { useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useQuery } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faChild,
  faEnvelope,
  faGear,
  fasChild,
  fasEnvelope,
  fasUser,
  faUser
} from 'lib-icons'

import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import { unreadCountsQuery } from '../messages/queries'
import { MessageContext } from '../messages/state'

import { useTranslation } from './i18n'
import { useSelectedGroup } from './selected-group'
import { UnitContext } from './unit'

export type NavItem = 'child' | 'staff' | 'messages' | 'settings'

export const bottomNavBarHeight = 60

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

const BottomText = ({ text, children, selected, onClick }: BottomTextProps) => (
  <FixedSpaceColumn spacing="3px" onClick={onClick}>
    <FixedSpaceRow justifyContent="center">{children}</FixedSpaceRow>
    <FixedSpaceRow justifyContent="space-evenly" alignItems="center">
      <IconText selected={selected}>{text}</IconText>
    </FixedSpaceRow>
  </FixedSpaceColumn>
)

export type BottomNavbarProps = {
  selected?: NavItem
}

export default function BottomNavbar({ selected }: BottomNavbarProps) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const { unitId } = useRequiredParams('unitId')
  const { groupRoute } = useSelectedGroup()

  const { unitInfoResponse } = useContext(UnitContext)
  const { user } = useContext(UserContext)
  const { data: unreadCounts = [] } = useQuery(unreadCountsQuery(unitId), {
    refetchOnMount: false
  })
  const { groupAccounts } = useContext(MessageContext)

  const groupAccountIds = groupAccounts.map(({ account }) => account.id)

  return renderResult(combine(unitInfoResponse, user), ([unit, user]) => (
    <>
      {/* Reserve navbar's height from the page, so that the fixed navbar doesn't hide anything */}
      <ReserveSpace />
      <Root>
        <Button data-qa="bottomnav-children">
          <BottomText
            text={i18n.common.children}
            selected={selected === 'child'}
            onClick={() =>
              selected !== 'child' && navigate(`${groupRoute}/child-attendance`)
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
              navigate(
                unit.features.includes('REALTIME_STAFF_ATTENDANCE')
                  ? `${groupRoute}/staff-attendance`
                  : `${groupRoute}/staff`
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
                navigate(
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
        {unit.features.includes('PUSH_NOTIFICATIONS') ? (
          <Button data-qa="bottomnav-settings">
            <BottomText
              text={i18n.common.settings}
              selected={selected === 'settings'}
              onClick={() =>
                selected !== 'settings' && navigate(`/units/${unitId}/settings`)
              }
            >
              <CustomIcon
                icon={selected === 'settings' ? fasGear : faGear}
                selected={selected === 'settings'}
              />
            </BottomText>
          </Button>
        ) : null}
      </Root>
    </>
  ))
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
