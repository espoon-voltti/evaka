// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'
import { useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import type { GroupInfo } from 'lib-common/generated/api-types/attendance'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import type { UUID } from 'lib-common/types'
import { Gap } from 'lib-components/white-space'

import { routes } from '../App'
import { UserContext } from '../auth/state'
import { unitInfoQuery } from '../units/queries'

import type { CountInfo } from './GroupSelector'
import { GroupSelectorBar } from './GroupSelectorBar'
import TopBar from './TopBar'

export type TopBarWithGroupSelectorProps = {
  unitId: DaycareId
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  includeSelectAll?: boolean
  toggleSearch?: () => void
  countInfo?: CountInfo | undefined
  allowedGroupIds?: UUID[] | undefined
}

export default React.memo(function TopBarWithGroupSelector({
  unitId,
  onChangeGroup,
  toggleSearch,
  selectedGroup,
  countInfo,
  includeSelectAll = true,
  allowedGroupIds = undefined
}: TopBarWithGroupSelectorProps) {
  const [, navigate] = useLocation()
  const { user } = useContext(UserContext)
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const topBarProps = useMemo(
    () =>
      combine(user, unitInfoResponse)
        .map(([user, unitInfo]) => {
          const title = unitInfo.name
          const onBack =
            user && user.unitIds.length > 1
              ? () => navigate(routes.unitList().value)
              : undefined
          return { title, onBack }
        })
        .getOrElse({ title: '' }),
    [navigate, user, unitInfoResponse]
  )

  const groups: GroupInfo[] = useMemo(
    () =>
      unitInfoResponse
        .map(({ groups }) => groups)
        .getOrElse([])
        .filter(
          (g) =>
            allowedGroupIds === undefined ||
            allowedGroupIds.findIndex((gid) => gid === g.id) > -1
        ),
    [unitInfoResponse, allowedGroupIds]
  )

  return (
    <>
      <TopBar {...topBarProps} unitId={unitId} />
      <Gap size="xxs" />
      <GroupSelectorBar
        unitId={unitId}
        selectedGroup={selectedGroup}
        onChangeGroup={onChangeGroup}
        onSearch={toggleSearch}
        countInfo={countInfo}
        groups={groups}
        includeSelectAll={includeSelectAll}
      />
    </>
  )
})
