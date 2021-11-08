// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { combine } from 'lib-common/api'
import { Staff } from 'lib-common/generated/api-types/attendance'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faLockOpenAlt } from 'lib-icons'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { pinLogout } from '../../api/auth'
import { UnitContext } from '../../state/unit'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

const UserContainer = styled.div`
  flex-basis: 60px;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;

  // smaller gap between user initials and the icon
  button {
    span + span {
      margin-left: ${defaultMargins.xs};
    }
  }
`

const getInitials = (u: Staff | undefined) =>
  u ? `${u.firstName.substring(0, 1)}${u.lastName.substring(0, 1)}` : ''

export const LoggedInUser = React.memo(function LoggedInUser() {
  const { user, refreshAuthStatus } = useContext(UserContext)
  const { unitInfoResponse } = useContext(UnitContext)

  const userName = useMemo(
    () =>
      combine(user, unitInfoResponse)
        .map(([user, unit]) =>
          user?.employeeId
            ? unit.staff.find(({ id }) => id === user.employeeId)
            : undefined
        )
        .map(getInitials)
        .getOrElse(''),
    [unitInfoResponse, user]
  )

  const [loggingOut, setLoggingOut] = useState(false)
  const logout = useCallback(() => {
    setLoggingOut(true)
    return pinLogout().then((res) => {
      setLoggingOut(false)
      if (res.isSuccess) {
        refreshAuthStatus()
      }
    })
  }, [refreshAuthStatus])

  return (
    <UserContainer>
      {renderResult(user, (u) =>
        u?.employeeId ? (
          <InlineButton
            icon={faLockOpenAlt}
            iconRight
            color={colors.greyscale.white}
            text={userName}
            disabled={loggingOut}
            onClick={logout}
            data-qa="logout-btn"
          />
        ) : null
      )}
    </UserContainer>
  )
})
