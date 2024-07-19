// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useQuery, useQueryResult } from 'lib-common/query'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasGear } from 'lib-icons'
import {
  faChild,
  faEnvelope,
  faGear,
  fasChild,
  fasEnvelope,
  fasUser,
  faUser
} from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { UserContext } from '../auth/state'
import { unreadCountsQuery } from '../messages/queries'
import { MessageContext } from '../messages/state'
import { unitInfoQuery } from '../units/queries'

import { useTranslation } from './i18n'
import { UnitOrGroup, toUnitOrGroup } from './unit-or-group'

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
  unitOrGroup: UnitOrGroup
  selected?: NavItem
}

export default function BottomNavbar({
  unitOrGroup,
  selected
}: BottomNavbarProps) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const { user } = useContext(UserContext)
  const { data: unreadCounts = [] } = useQuery(unreadCountsQuery({ unitId }), {
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
              selected !== 'child' &&
              navigate(routes.childAttendances(unitOrGroup).value)
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
                (unit.features.includes('REALTIME_STAFF_ATTENDANCE')
                  ? routes.staffAttendances(unitOrGroup, 'absent')
                  : routes.staff(unitOrGroup)
                ).value
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
                  (user?.pinLoginActive
                    ? routes.messages(
                        toUnitOrGroup({
                          unitId,
                          groupId: unit.groups[0]?.id
                        })
                      )
                    : routes.unreadMessages(
                        toUnitOrGroup({
                          unitId,
                          groupId: unit.groups[0]?.id
                        })
                      )
                  ).value
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
                selected !== 'settings' &&
                navigate(routes.settings(unitId).value)
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
