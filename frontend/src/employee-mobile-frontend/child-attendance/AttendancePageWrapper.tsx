// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { animated, useSpring } from '@react-spring/web'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  AttendanceChild,
  GroupInfo
} from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { ContentArea } from 'lib-components/layout/Container'
import colors from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import FreeTextSearch from '../common/FreeTextSearch'
import { CountInfo } from '../common/GroupSelector'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import { UnitContext } from '../common/unit'
import { zIndex } from '../constants'
import { ChildAttendanceUIState, mapChildAttendanceUIState } from '../types'

import AttendanceList from './AttendanceList'
import ChildList from './ChildList'
import { attendanceStatusesQuery, childrenQuery } from './queries'
import { AttendanceStatuses, childAttendanceStatus } from './utils'

export default React.memo(function AttendancePageWrapper() {
  const { unitId, groupId, attendanceStatus } = useNonNullableParams<{
    unitId: string
    groupId: string
    attendanceStatus: ChildAttendanceUIState
  }>()
  const navigate = useNavigate()
  const { i18n } = useTranslation()
  const { unitInfoResponse } = useContext(UnitContext)

  const selectedGroup = useMemo(
    () =>
      groupId === 'all'
        ? undefined
        : unitInfoResponse
            .map((res) => res.groups.find((g) => g.id === groupId))
            .getOrElse(undefined),
    [groupId, unitInfoResponse]
  )

  const unitChildren = useQueryResult(childrenQuery(unitId))
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId))

  const [showSearch, setShowSearch] = useState<boolean>(false)

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      navigate(
        `/units/${unitId}/groups/${
          group?.id ?? 'all'
        }/child-attendance/list/${attendanceStatus}`
      )
    },
    [navigate, unitId, attendanceStatus]
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
        {renderResult(
          combine(unitChildren, attendanceStatuses),
          ([children, attendanceStatuses]) => (
            <AttendanceList
              unitId={unitId}
              groupId={groupId}
              activeStatus={mapChildAttendanceUIState(attendanceStatus)}
              unitChildren={children}
              attendanceStatuses={attendanceStatuses}
            />
          )
        )}
      </PageWithNavigation>
    </>
  )
})

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
