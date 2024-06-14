// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-mobile-frontend/common/i18n'
import { combine } from 'lib-common/api'
import { Staff } from 'lib-common/generated/api-types/attendance'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { defaultMargins } from 'lib-components/white-space'
import { faLockOpenAlt, faTimes } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { UserContext } from '../../auth/state'
import { unitInfoQuery } from '../../units/queries'

import { TopBarIconContainer } from './TopBarIconContainer'
import { UserMenu } from './UserMenu'

const UserContainer = styled(TopBarIconContainer)`
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

export const LoggedInUser = React.memo(function LoggedInUser({
  unitId
}: {
  unitId: UUID | undefined
}) {
  const { user, refreshAuthStatus } = useContext(UserContext)

  const unitInfoResponse = useQueryResult(
    queryOrDefault(unitInfoQuery, null)(unitId ? { unitId } : null)
  )

  const userNames = useMemo(
    () =>
      combine(user, unitInfoResponse)
        .map(([user, unitInfo]) => {
          const employeeId = user?.employeeId

          if (!employeeId) {
            return undefined
          }

          return unitInfo?.staff.find(({ id }) => id === employeeId)
        })
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

  const { i18n } = useTranslation()

  return (
    <UserContainer data-qa="top-bar-user">
      {renderResult(user, (u) =>
        u?.employeeId && u?.pinLoginActive ? (
          menuOpen ? (
            <>
              <IconOnlyButton
                icon={faTimes}
                color="white"
                onClick={toggleMenu}
                data-qa="close-user-menu-btn"
                aria-label={i18n.common.close}
              />
              <UserMenu
                name={userNames.name}
                onLogoutSuccess={onLogoutSuccess}
              />
            </>
          ) : (
            <Button
              appearance="inline"
              order="text-icon"
              icon={faLockOpenAlt}
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
