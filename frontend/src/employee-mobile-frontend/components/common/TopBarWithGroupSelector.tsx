// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UserContext } from 'employee-mobile-frontend/state/user'
import { combine } from 'lib-common/api'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { Gap } from 'lib-components/white-space'
import React, { useContext, useMemo } from 'react'
import { useHistory } from 'react-router'
import { UnitContext } from '../../state/unit'
import { CountInfo } from './GroupSelector'
import { GroupSelectorBar } from './GroupSelectorBar'
import TopBar from './TopBar'

interface Props {
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  toggleSearch?: () => void
  countInfo?: CountInfo | undefined
}

export default React.memo(function TopBarWithGroupSelector({
  onChangeGroup,
  toggleSearch,
  selectedGroup,
  countInfo
}: Props) {
  const history = useHistory()
  const { user } = useContext(UserContext)
  const { unitInfoResponse } = useContext(UnitContext)

  const topBarProps = useMemo(() => {
    return combine(user, unitInfoResponse)
      .map(([user, unitInfo]) => {
        const title = unitInfo.name
        const onBack =
          user && user.unitIds.length > 1
            ? () => history.push('/units')
            : undefined
        return { title, onBack }
      })
      .getOrElse({ title: '' })
  }, [history, user, unitInfoResponse])

  const groups: GroupInfo[] = useMemo(
    () => unitInfoResponse.map(({ groups }) => groups).getOrElse([]),
    [unitInfoResponse]
  )

  return (
    <>
      <TopBar {...topBarProps} />
      <Gap size="xxs" />
      <GroupSelectorBar
        selectedGroup={selectedGroup}
        onChangeGroup={onChangeGroup}
        onSearch={toggleSearch}
        countInfo={countInfo}
        groups={groups}
      />
    </>
  )
})
