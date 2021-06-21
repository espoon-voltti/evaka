// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import {
  matchPath,
  Redirect,
  Route,
  Switch,
  useHistory,
  useParams,
  useRouteMatch
} from 'react-router-dom'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'

import Tabs from 'lib-components/molecules/Tabs'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'

import AttendanceComingPage from './AttendanceComingPage'
import AttendancePresentPage from './AttendancePresentPage'
import AttendanceDepartedPage from './AttendanceDepartedPage'
import AttendanceAbsentPage from './AttendanceAbsentPage'
import AttendanceList from './AttendanceList'
import {
  AttendanceChild,
  getDaycareAttendances,
  Group
} from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import { useTranslation } from '../../state/i18n'
import FreeTextSearch from '../common/FreeTextSearch'
import TopBar from '../common/TopBar'
import BottomNavbar, { NavItem } from '../common/BottomNavbar'
import { UnitStaffAttendance } from 'lib-common/api-types/staffAttendances'
import { Loading, Result, combine } from 'lib-common/api'
import { getUnitStaffAttendances } from 'employee-mobile-frontend/api/staffAttendances'
import { staffAttendanceForGroupOrUnit } from '../../utils/staffAttendances'

export interface Props {
  onNavigate: (page: NavItem) => void
}

export default React.memo(function AttendancePageWrapper({
  onNavigate
}: Props) {
  const { i18n } = useTranslation()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string
  }>()
  const { path, url } = useRouteMatch()
  const history = useHistory()

  const currentPage = matchPath<{ page: string }>(history.location.pathname, {
    path: `${path}/:page`
  })?.params.page

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )
  const [staffAttendancesResponse, setStaffAttendancesResponse] = useState<
    Result<UnitStaffAttendance>
  >(Loading.of())

  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [freeText, setFreeText] = useState<string>('')
  const [searchResults, setSearchResults] = useState<AttendanceChild[]>([])
  const [selectedGroup, setSelectedGroup] = useState<Group | undefined>(
    undefined
  )

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )
  const loadStaffAttendances = useRestApi(
    getUnitStaffAttendances,
    setStaffAttendancesResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [loadDaycareAttendances, groupIdOrAll, unitId, currentPage])

  useEffect(() => {
    loadStaffAttendances(unitId)
  }, [unitId, loadStaffAttendances])

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

  useEffect(() => {
    const selectedGroup =
      attendanceResponse.isSuccess &&
      groupIdOrAll !== 'all' &&
      attendanceResponse.value.unit.groups.find(
        (elem: Group) => elem.id === groupIdOrAll
      )
    if (selectedGroup) {
      setSelectedGroup(selectedGroup)
    }
    return () => {
      setSelectedGroup(undefined)
    }
  }, [attendanceResponse, groupIdOrAll])

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

  function changeGroup(group: Group | undefined) {
    if (attendanceResponse.isSuccess) {
      if (currentPage && group !== undefined) {
        history.push(
          `/units/${attendanceResponse.value.unit.id}/attendance/${group.id}/${currentPage}`
        )
        setSelectedGroup(group)
      } else if (currentPage && group === undefined) {
        history.push(
          `/units/${attendanceResponse.value.unit.id}/attendance/all/${currentPage}`
        )
        setSelectedGroup(undefined)
      }
    }
  }

  const containerSpring = useSpring<{ x: number }>({ x: showSearch ? 1 : 0 })

  return combine(
    attendanceResponse,
    staffAttendancesResponse.map((staffAttendances) =>
      staffAttendanceForGroupOrUnit(
        staffAttendances,
        groupIdOrAll === 'all' ? undefined : groupIdOrAll
      )
    )
  ).mapAll({
    failure() {
      return <ErrorSegment />
    },
    loading() {
      return <Loader />
    },
    success([attendance, staffAttendances]) {
      return (
        <>
          <SearchBar
            style={{
              height: containerSpring.x.interpolate((x) => `${100 * x}%`)
            }}
          >
            <NoMarginTitle size={1} centered smaller bold data-qa="unit-name">
              {attendance.unit.name}{' '}
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
                groups={[]}
                showAll
              />
            </ContentArea>
          </SearchBar>
          <TopBar
            unitName={attendance.unit.name}
            selectedGroup={selectedGroup}
            onChangeGroup={changeGroup}
            onSearch={() => setShowSearch(!showSearch)}
          />
          <Tabs tabs={tabs} mobile />

          <ContentArea
            fullHeight
            opaque={false}
            paddingVertical={'s'}
            paddingHorizontal={'zero'}
          >
            <Switch>
              <Route
                exact
                path={`${path}/coming`}
                render={() => (
                  <AttendanceComingPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route
                exact
                path={`${path}/present`}
                render={() => (
                  <AttendancePresentPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route
                exact
                path={`${path}/departed`}
                render={() => (
                  <AttendanceDepartedPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route
                exact
                path={`${path}/absent`}
                render={() => (
                  <AttendanceAbsentPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Redirect to={`${path}/coming`} />
            </Switch>
          </ContentArea>
          <BottomNavbar
            selected="child"
            staffCount={staffAttendances}
            onChange={onNavigate}
          />
        </>
      )
    }
  })
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
  box-shadow: 0px 2px 6px 0px ${colors.greyscale.lighter};
  position: relative;
  z-index: 1;

  button {
    margin-left: ${defaultMargins.m};
    color: ${colors.greyscale.white};
  }
`

const Bold = styled.div`
  font-weight: 600;
`

const SearchBar = animated(styled.div`
  position: absolute;
  background: ${colors.greyscale.lightest};
  width: 100vw;
  overflow: hidden;
  z-index: 2;
`)
