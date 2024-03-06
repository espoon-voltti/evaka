// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { animated, useSpring } from '@react-spring/web'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import {
  Outlet,
  useLocation,
  useNavigate,
  useOutletContext
} from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  AttendanceChild,
  GroupInfo
} from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import FreeTextSearch from '../common/FreeTextSearch'
import { CountInfo } from '../common/GroupSelector'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'
import { zIndex } from '../constants'
import { ChildAttendanceUIState, parseChildAttendanceUiState } from '../types'

import ChildList from './ChildList'
import { attendanceStatusesQuery, childrenQuery } from './queries'
import { AttendanceStatuses, childAttendanceStatus } from './utils'

export interface TabItem {
  id: string
  label: string
  link: string
}

export default React.memo(function AttendancePageWrapper() {
  const { unitId, attendanceStatus } = useRequiredParams('unitId', [
    'attendanceStatus',
    parseChildAttendanceUiState
  ])
  const navigate = useNavigate()
  const location = useLocation()
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)
  const { selectedGroupId } = useSelectedGroup()

  const selectedGroup = useMemo(
    () =>
      selectedGroupId.type === 'all'
        ? undefined
        : unitInfoResponse
            .map((res) => res.groups.find((g) => g.id === selectedGroupId.id))
            .getOrElse(undefined),
    [selectedGroupId, unitInfoResponse]
  )

  const unitChildren = useQueryResult(childrenQuery(unitId))
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId))

  const [showSearch, setShowSearch] = useState<boolean>(false)

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      const newGroupRoute = location.pathname.split('/')
      newGroupRoute[4] = `${group?.id ?? 'all'}`

      navigate(newGroupRoute.join('/'))
    },
    [navigate, location.pathname]
  )
  const { groupRoute } = useSelectedGroup()
  const tabs = useMemo(() => {
    const url = `${groupRoute}/child-attendance`

    return [
      {
        id: 'today',
        link: `${url}/list`,
        label: i18n.attendances.views.TODAY
      },
      {
        id: 'confirmed-days',
        link: `${url}/daylist`,
        label: i18n.attendances.views.NEXT_DAYS
      }
    ]
  }, [i18n, groupRoute])

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
          unitId={unitId}
          show={showSearch}
          toggleShow={toggleSearch}
          unitChildren={unitChildren.value}
          attendanceStatuses={attendanceStatuses.value}
        />
      )}
      <PageWithNavigation
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
                  unitId: unitId,
                  unitChildren: children,
                  attendanceStatuses,
                  attendanceStatus
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
  unitId: string
  unitChildren: AttendanceChild[]
  attendanceStatuses: AttendanceStatuses
  attendanceStatus: ChildAttendanceUIState
}
export const useAttendanceContext = () => useOutletContext<AttendanceContext>()

const ChildSearch = React.memo(function Search({
  unitId,
  show,
  toggleShow,
  unitChildren,
  attendanceStatuses
}: {
  unitId: string
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
        <ChildList unitId={unitId} items={searchResults} />
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
