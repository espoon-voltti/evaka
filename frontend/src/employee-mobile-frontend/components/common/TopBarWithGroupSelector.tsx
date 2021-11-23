// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { Gap } from 'lib-components/white-space'
import React from 'react'
import { CountInfo } from './GroupSelector'
import { GroupSelectorBar } from './GroupSelectorBar'
import { TopBar } from './TopBar'

interface Props {
  title: string
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  toggleSearch?: () => void
  countInfo?: CountInfo
  groups?: GroupInfo[]
}

export const TopBarWithGroupSelector = React.memo(
  function TopBarWithGroupSelector({
    countInfo,
    onChangeGroup,
    toggleSearch: onSearch,
    selectedGroup,
    title,
    groups
  }: Props) {
    return (
      <>
        <TopBar title={title} />
        <Gap size="xxs" />
        <GroupSelectorBar
          selectedGroup={selectedGroup}
          onChangeGroup={onChangeGroup}
          onSearch={onSearch}
          countInfo={countInfo}
          groups={groups}
        />
      </>
    )
  }
)
