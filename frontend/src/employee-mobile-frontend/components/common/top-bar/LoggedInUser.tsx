// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { combine } from 'lib-common/api'
import { Staff } from 'lib-common/generated/api-types/attendance'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faLockOpenAlt, faTimes } from 'lib-icons'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { UnitContext } from '../../../state/unit'
import { UserContext } from '../../../state/user'
import { renderResult } from '../../async-rendering'
import { TopBarIconContainer } from './TopBarIconContainer'
import { UserMenu } from './UserMenu'

const UserContainer = styled(TopBarIconContainer)<{ active?: boolean }>`
  border-bottom-style: solid;
  border-bottom-width: 4px;
  border-bottom-color: ${({ active }) =>
    active ? colors.greyscale.white : 'transparent'};

  // smaller gap between user initials and the icon
  button {
    span + svg {
      margin-left: ${defaultMargins.xs};
    }
  }
`

const getUserName = (u: Staff | undefined) => {
  const names = [u?.firstName, u?.lastName].filter(Boolean)
  return {
    initials: names.map((s) => s?.substring(0, 1)).join(''),
    name: names.join(' ')
  }
}

export const LoggedInUser = React.memo(function LoggedInUser() {
  const { user, refreshAuthStatus } = useContext(UserContext)
  const { unitInfoResponse } = useContext(UnitContext)

  const userNames = useMemo(
    () =>
      combine(user, unitInfoResponse)
        .map(([user, unit]) =>
          user?.employeeId
            ? unit.staff.find(({ id }) => id === user.employeeId)
            : undefined
        )
        .map(getUserName)
        .getOrElse(getUserName(undefined)),
    [unitInfoResponse, user]
  )

  const onLogoutSuccess = useCallback(() => {
    setMenuOpen(false)
    refreshAuthStatus()
  }, [refreshAuthStatus])

  const [menuOpen, setMenuOpen] = useState(false)
  const toggleMenu = useCallback(() => setMenuOpen((prev) => !prev), [])

  return (
    <UserContainer active={menuOpen} data-qa="top-bar-user">
      {renderResult(user, (u) =>
        u?.employeeId && u?.pinLoginActive ? (
          menuOpen ? (
            <>
              <IconButton
                icon={faTimes}
                white
                onClick={toggleMenu}
                data-qa="close-user-menu-btn"
              />
              <UserMenu
                name={userNames.name}
                onLogoutSuccess={onLogoutSuccess}
              />
            </>
          ) : (
            <InlineButton
              icon={faLockOpenAlt}
              iconRight
              color={colors.greyscale.white}
              text={userNames.initials}
              onClick={toggleMenu}
              data-qa="open-user-menu-btn"
            />
          )
        ) : null
      )}
    </UserContainer>
  )
})
