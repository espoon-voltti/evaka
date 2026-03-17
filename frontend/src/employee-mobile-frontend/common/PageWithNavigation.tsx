// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import type { BottomNavbarProps } from './BottomNavbar'
import BottomNavbar from './BottomNavbar'
import type { TopBarWithGroupSelectorProps } from './TopBarWithGroupSelector'
import TopBarWithGroupSelector from './TopBarWithGroupSelector'

const FlexibleDiv = styled.div`
  flex-grow: 1;
  display: block;
`

type PageWithNavigation = BottomNavbarProps &
  Omit<TopBarWithGroupSelectorProps, 'unitId'> & {
    children: React.ReactNode
  }

export const PageWithNavigation: React.FC<PageWithNavigation> = ({
  unitOrGroup,
  selected,
  onChangeGroup,
  toggleSearch,
  selectedGroup,
  countInfo,
  includeSelectAll = true,
  allowedGroupIds = undefined,
  shiftCareSelected,
  onSelectShiftCare,
  children
}) => (
  <FixedSpaceColumn $spacing="zero" style={{ height: '100vh' }}>
    <TopBarWithGroupSelector
      unitId={unitOrGroup.unitId}
      onChangeGroup={onChangeGroup}
      toggleSearch={toggleSearch}
      selectedGroup={selectedGroup}
      countInfo={countInfo}
      includeSelectAll={includeSelectAll}
      allowedGroupIds={allowedGroupIds}
      shiftCareSelected={shiftCareSelected}
      onSelectShiftCare={onSelectShiftCare}
    />
    <FlexibleDiv>{children}</FlexibleDiv>
    <BottomNavbar selected={selected} unitOrGroup={unitOrGroup} />
  </FixedSpaceColumn>
)
