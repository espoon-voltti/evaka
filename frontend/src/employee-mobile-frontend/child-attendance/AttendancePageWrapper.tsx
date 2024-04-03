// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { animated, useSpring } from '@react-spring/web'
import React, { useCallback, useMemo, useState } from 'react'
import { Outlet, useNavigate, useOutletContext } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  AttendanceChild,
  GroupInfo
} from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'
import colors from 'lib-customizations/common'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import FreeTextSearch from '../common/FreeTextSearch'
import { CountInfo } from '../common/GroupSelector'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import { toUnitOrGroup, UnitOrGroup } from '../common/unit-or-group'
import { zIndex } from '../constants'
import { unitInfoQuery } from '../units/queries'

import ChildList from './ChildList'
import { attendanceStatusesQuery, childrenQuery } from './queries'
import { AttendanceStatuses, childAttendanceStatus } from './utils'

export interface TabItem {
  id: string
  label: string
  link: string
}

export default React.memo(function AttendancePageWrapper({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const selectedGroup = useMemo(
    () =>
      unitOrGroup.type === 'unit'
        ? undefined
        : unitInfoResponse
            .map((res) => res.groups.find((g) => g.id === unitOrGroup.id))
            .getOrElse(undefined),
    [unitOrGroup, unitInfoResponse]
  )

  const unitChildren = useQueryResult(childrenQuery(unitId))
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId))

  const [showSearch, setShowSearch] = useState<boolean>(false)

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      navigate(
        routes.childAttendances(toUnitOrGroup({ unitId, groupId: group?.id }))
          .value
      )
    },
    [navigate, unitId]
  )
  const tabs = useMemo(
    () => [
      {
        id: 'today',
        link: routes.childAttendances(unitOrGroup),
        label: i18n.attendances.views.TODAY
      },
      {
        id: 'confirmed-days',
        link: routes.childAttendanceDaylist(unitOrGroup),
        label: i18n.attendances.views.NEXT_DAYS
      }
    ],
    [i18n, unitOrGroup]
  )

  const toggleSearch = useCallback(() => setShowSearch((show) => !show), [])

  const countInfo: CountInfo | undefined = useMemo(
    () =>
      combine(unitChildren, attendanceStatuses)
        .map(([children, attendanceStatuses]) => ({
          getTotalCount: (groupId: string | undefined) =>
            groupId === undefined
              ? children.length
              : children.filter((child) => child.groupId === groupId).length,
          getPresentCount: (groupId: string | undefined) => {
            const presentChildren = children.filter(
              (child) =>
                childAttendanceStatus(child, attendanceStatuses).status ===
                'PRESENT'
            )
            return groupId === undefined
              ? presentChildren.length
              : presentChildren.filter((child) => child.groupId === groupId)
                  .length
          },
          infoText: i18n.attendances.chooseGroupInfo
        }))
        .getOrElse(undefined),
    [attendanceStatuses, i18n, unitChildren]
  )

  return (
    <>
      {unitChildren.isSuccess && attendanceStatuses.isSuccess && (
        <ChildSearch
          unitOrGroup={unitOrGroup}
          show={showSearch}
          toggleShow={toggleSearch}
          unitChildren={unitChildren.value}
          attendanceStatuses={attendanceStatuses.value}
        />
      )}
      <PageWithNavigation
        unitOrGroup={unitOrGroup}
        selected="child"
        selectedGroup={selectedGroup}
        onChangeGroup={changeGroup}
        toggleSearch={toggleSearch}
        countInfo={countInfo}
      >
        <TabLinks tabs={tabs} mobile sticky />
        {renderResult(
          combine(unitChildren, attendanceStatuses),
          ([children, attendanceStatuses]) => (
            <Outlet
              context={
                {
                  unitChildren: children,
                  attendanceStatuses
                } satisfies AttendanceContext
              }
            />
          )
        )}
      </PageWithNavigation>
    </>
  )
})

export type AttendanceContext = {
  unitChildren: AttendanceChild[]
  attendanceStatuses: AttendanceStatuses
}
export const useAttendanceContext = () => useOutletContext<AttendanceContext>()

const ChildSearch = React.memo(function Search({
  unitOrGroup,
  show,
  toggleShow,
  unitChildren,
  attendanceStatuses
}: {
  unitOrGroup: UnitOrGroup
  show: boolean
  toggleShow: () => void
  unitChildren: AttendanceChild[]
  attendanceStatuses: AttendanceStatuses
}) {
  const { i18n } = useTranslation()
  const containerSpring = useSpring<{ x: number }>({ x: show ? 1 : 0 })
  const [freeText, setFreeText] = useState<string>('')

  const searchResults = useMemo(
    () =>
      freeText === ''
        ? []
        : unitChildren
            .filter(
              (ac) =>
                ac.firstName.toLowerCase().includes(freeText.toLowerCase()) ||
                ac.lastName.toLowerCase().includes(freeText.toLowerCase())
            )
            .map((child) => ({
              ...child,
              status: childAttendanceStatus(child, attendanceStatuses).status
            })),
    [attendanceStatuses, freeText, unitChildren]
  )

  return (
    <SearchContainer
      style={{ height: containerSpring.x.to((x) => `${100 * x}%`) }}
    >
      <ContentArea
        opaque={false}
        paddingVertical="zero"
        paddingHorizontal="zero"
      >
        <FreeTextSearch
          value={freeText}
          setValue={setFreeText}
          placeholder={i18n.attendances.searchPlaceholder}
          background={colors.grayscale.g0}
          setShowSearch={toggleShow}
          searchResults={searchResults}
        />
        <ChildList unitOrGroup={unitOrGroup} items={searchResults} />
      </ContentArea>
    </SearchContainer>
  )
})

const SearchContainer = animated(styled.div`
  position: absolute;
  background: ${colors.grayscale.g4};
  width: 100vw;
  overflow: hidden;
  z-index: ${zIndex.searchBar};
`)
