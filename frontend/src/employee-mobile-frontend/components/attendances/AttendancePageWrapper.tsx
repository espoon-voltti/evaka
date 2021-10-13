// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { combine } from 'lib-common/api'
import { Child, GroupInfo } from 'lib-common/generated/api-types/attendance'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import Tabs from 'lib-components/molecules/Tabs'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'
import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { animated, useSpring } from 'react-spring'
import styled from 'styled-components'
import { ChildAttendanceContext } from '../../state/child-attendance'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { ChildAttendanceUIState, mapChildAttendanceUIState } from '../../types'
import { renderResult } from '../async-rendering'
import BottomNavbar from '../common/BottomNavbar'
import FreeTextSearch from '../common/FreeTextSearch'
import TopBar from '../common/TopBar'
import AttendanceList from './AttendanceList'

export default React.memo(function AttendancePageWrapper() {
  const { i18n } = useTranslation()
  const {
    unitId,
    groupId: groupIdOrAll,
    attendanceStatus
  } = useParams<{
    unitId: string
    groupId: string
    attendanceStatus: ChildAttendanceUIState
  }>()
  const history = useHistory()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(() => reloadUnitInfo(true), [reloadUnitInfo])

  const selectedGroup =
    groupIdOrAll === 'all'
      ? undefined
      : unitInfoResponse
          .map((res) => res.groups.find((g) => g.id === groupIdOrAll))
          .getOrElse(undefined)

  const { attendanceResponse, reloadAttendances } = useContext(
    ChildAttendanceContext
  )

  useEffect(() => reloadAttendances(true), [reloadAttendances])

  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [freeText, setFreeText] = useState<string>('')
  const [searchResults, setSearchResults] = useState<Child[]>([])

  useEffect(() => {
    if (freeText === '') {
      setSearchResults([])
    } else if (attendanceResponse.isSuccess) {
      const filteredData = attendanceResponse.value.children.filter(
        (ac) =>
          ac.firstName.toLowerCase().includes(freeText.toLowerCase()) ||
          ac.lastName.toLowerCase().includes(freeText.toLowerCase())
      )
      setSearchResults(filteredData)
    }
  }, [freeText, attendanceResponse])

  const totalAttendances = attendanceResponse.isSuccess
    ? groupIdOrAll === 'all'
      ? attendanceResponse.value.children.length
      : attendanceResponse.value.children.filter(
          (ac) => ac.groupId === groupIdOrAll
        ).length
    : 0
  const totalComing = attendanceResponse
    .map((res) =>
      groupIdOrAll === 'all'
        ? res.children.filter((ac) => ac.status === 'COMING').length
        : res.children
            .filter((ac) => ac.groupId === groupIdOrAll)
            .filter((ac) => ac.status === 'COMING').length
    )
    .getOrElse(0)
  const totalPresent = attendanceResponse
    .map((res) =>
      groupIdOrAll === 'all'
        ? res.children.filter((ac) => ac.status === 'PRESENT').length
        : res.children
            .filter((ac) => ac.groupId === groupIdOrAll)
            .filter((ac) => ac.status === 'PRESENT').length
    )
    .getOrElse(0)
  const totalDeparted = attendanceResponse
    .map((res) =>
      groupIdOrAll === 'all'
        ? res.children.filter((ac) => ac.status === 'DEPARTED').length
        : res.children
            .filter((ac) => ac.groupId === groupIdOrAll)
            .filter((ac) => ac.status === 'DEPARTED').length
    )
    .getOrElse(0)
  const totalAbsent = attendanceResponse
    .map((res) =>
      groupIdOrAll === 'all'
        ? res.children.filter((ac) => ac.status === 'ABSENT').length
        : res.children
            .filter((ac) => ac.groupId === groupIdOrAll)
            .filter((ac) => ac.status === 'ABSENT').length
    )
    .getOrElse(0)

  const url = `/units/${unitId}/groups/${groupIdOrAll}/child-attendance/list`
  const tabs = [
    {
      id: 'coming',
      link: `${url}/coming`,
      label: (
        <Bold>
          {i18n.attendances.types.COMING}
          <br />
          {`${totalComing}/${totalAttendances}`}
        </Bold>
      )
    },
    {
      id: 'present',
      link: `${url}/present`,
      label: (
        <Bold>
          {i18n.attendances.types.PRESENT}
          <br />
          {`${totalPresent}/${totalAttendances}`}
        </Bold>
      )
    },
    {
      id: 'departed',
      link: `${url}/departed`,
      label: (
        <Bold>
          {i18n.attendances.types.DEPARTED}
          <br />
          {`${totalDeparted}/${totalAttendances}`}
        </Bold>
      )
    },
    {
      id: 'absent',
      link: `${url}/absent`,
      label: (
        <Bold>
          {i18n.attendances.types.ABSENT}
          <br />
          {`${totalAbsent}/${totalAttendances}`}
        </Bold>
      )
    }
  ]

  function changeGroup(group: GroupInfo | undefined) {
    history.push(
      `/units/${unitId}/groups/${
        group?.id ?? 'all'
      }/child-attendance/${attendanceStatus}/list`
    )
  }

  const containerSpring = useSpring<{ x: number }>({ x: showSearch ? 1 : 0 })

  return renderResult(
    combine(unitInfoResponse, attendanceResponse),
    ([unitInfo, attendance]) => (
      <>
        <SearchBar
          style={{
            height: containerSpring.x.interpolate((x) => `${100 * x}%`)
          }}
        >
          <NoMarginTitle size={1} centered smaller bold data-qa="unit-name">
            {unitInfo.name}{' '}
            <IconButton
              onClick={() => setShowSearch(!showSearch)}
              icon={faTimes}
            />
          </NoMarginTitle>
          <ContentArea
            opaque={false}
            paddingVertical={'zero'}
            paddingHorizontal={'zero'}
          >
            <FreeTextSearch
              value={freeText}
              setValue={setFreeText}
              placeholder={i18n.attendances.searchPlaceholder}
              background={colors.greyscale.white}
              setShowSearch={setShowSearch}
              searchResults={searchResults}
            />
            <AttendanceList
              attendanceChildren={searchResults}
              groupsNotes={attendance.groupNotes}
              showAll
            />
          </ContentArea>
        </SearchBar>
        <TopBar
          unitName={unitInfo.name}
          selectedGroup={selectedGroup}
          onChangeGroup={changeGroup}
          onSearch={() => setShowSearch(!showSearch)}
          countInfo={{
            getTotalCount: (groupId) =>
              attendanceResponse
                .map((res) =>
                  groupId === undefined
                    ? res.children.length
                    : res.children.filter((child) => child.groupId === groupId)
                        .length
                )
                .getOrElse(0),
            getPresentCount: (groupId) =>
              attendanceResponse
                .map((res) =>
                  res.children.filter((child) => child.status === 'PRESENT')
                )
                .map((children) =>
                  groupId === undefined
                    ? children.length
                    : children.filter((child) => child.groupId === groupId)
                        .length
                )
                .getOrElse(0),
            infoText: i18n.attendances.chooseGroupInfo
          }}
        />
        <Tabs tabs={tabs} mobile />

        <ContentArea
          opaque={false}
          paddingVertical={'zero'}
          paddingHorizontal={'zero'}
        >
          <AttendanceList
            attendanceChildren={attendance.children}
            groupsNotes={attendance.groupNotes}
            type={mapChildAttendanceUIState(attendanceStatus)}
          />
        </ContentArea>
        <BottomNavbar selected="child" />
      </>
    )
  )
})

const NoMarginTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
  padding-top: ${defaultMargins.s};
  padding-bottom: ${defaultMargins.s};
  display: flex;
  justify-content: center;
  align-items: center;
  background: ${colors.blues.primary};
  color: ${colors.greyscale.white};
  box-shadow: 0 2px 6px 0 ${colors.greyscale.lighter};
  position: relative;
  z-index: 1;

  button {
    margin-left: ${defaultMargins.m};
    color: ${colors.greyscale.white};
  }
`

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};
`

const SearchBar = animated(styled.div`
  position: absolute;
  background: ${colors.greyscale.lightest};
  width: 100vw;
  overflow: hidden;
  z-index: 2;
`)
