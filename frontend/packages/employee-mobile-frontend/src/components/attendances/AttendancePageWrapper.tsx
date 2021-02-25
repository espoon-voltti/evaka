// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import {
  Redirect,
  Route,
  Switch,
  useHistory,
  useLocation,
  useParams
} from 'react-router-dom'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'

import Tabs from '@evaka/lib-components/src/molecules/Tabs'
import Title from '@evaka/lib-components/src/atoms/Title'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import colors from '@evaka/lib-components/src/colors'
import { faAngleDown, faAngleUp, faSearch, faTimes } from '@evaka/lib-icons'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'

import AttendanceComingPage from './AttendanceComingPage'
import AttendancePresentPage from './AttendancePresentPage'
import AttendanceDepartedPage from './AttendanceDepartedPage'
import AttendanceAbsentPage from './AttendanceAbsentPage'
import AttendanceList from './AttendanceList'
import { AttendanceChild, getDaycareAttendances, Group } from '~api/attendances'
import { AttendanceUIContext } from '~state/attendance-ui'
import { useTranslation } from '~state/i18n'
import FreeTextSearch from '~components/common/FreeTextSearch'
import GroupSelector from './GroupSelector'

export default React.memo(function AttendancePageWrapper() {
  const { i18n } = useTranslation()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string
  }>()
  const location = useLocation()
  const history = useHistory()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [showGroupSelector, setShowGroupSelector] = useState<boolean>(false)
  const [freeText, setFreeText] = useState<string>('')
  const [searchResults, setSearchResults] = useState<AttendanceChild[]>([])
  const [selectedGroup, setSelectedGroup] = useState<Group | undefined>(
    undefined
  )

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [groupIdOrAll])

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
  }, [freeText])

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
  }, [attendanceResponse])

  const currentPage = location.pathname
    .split('/')
    .filter((elem) => elem.length > 0)
    .pop()
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
      link: `/units/${unitId}/attendance/${groupIdOrAll}/coming`,
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
      link: `/units/${unitId}/attendance/${groupIdOrAll}/present`,
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
      link: `/units/${unitId}/attendance/${groupIdOrAll}/departed`,
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
      link: `/units/${unitId}/attendance/${groupIdOrAll}/absent`,
      label: (
        <Bold>
          {i18n.attendances.types.ABSENT}
          <br />
          {`${totalAbsent}/${totalAttendances}`}
        </Bold>
      )
    }
  ]

  function RedirectToUnitPage() {
    return <Redirect to={`/units/${unitId}/attendance/all/coming`} />
  }

  function changeGroup(groupOrAll: Group | 'all') {
    if (attendanceResponse.isSuccess) {
      if (currentPage && groupOrAll !== 'all') {
        history.push(
          `/units/${attendanceResponse.value.unit.id}/attendance/${groupOrAll.id}/${currentPage}`
        )
        setSelectedGroup(groupOrAll)
      } else if (currentPage && groupOrAll === 'all') {
        history.push(
          `/units/${attendanceResponse.value.unit.id}/attendance/all/${currentPage}`
        )
        setSelectedGroup(undefined)
      }
      setShowGroupSelector(false)
    }
  }

  const containerSpring = useSpring<{ x: number }>({ x: showSearch ? 1 : 0 })
  const groupSelectorSpring = useSpring<{ x: number }>({
    x: showGroupSelector ? 1 : 0,
    config: { duration: 100 }
  })

  return (
    <Fragment>
      {attendanceResponse.isSuccess && (
        <Fragment>
          <SearchBar
            style={{
              height: containerSpring.x.interpolate((x) => `${100 * x}%`)
            }}
          >
            <NoMarginTitle size={1} centered smaller bold>
              {attendanceResponse.value.unit.name}{' '}
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
              <AttendanceList attendanceChildren={searchResults} showAll />
            </ContentArea>
          </SearchBar>
          <Name>
            <NoMarginTitle size={1} centered smaller bold>
              {attendanceResponse.value.unit.name}{' '}
              <IconButton
                onClick={() => setShowSearch(!showSearch)}
                icon={faSearch}
              />
            </NoMarginTitle>
          </Name>
          <GroupSelectorWrapper
            style={{
              maxHeight: groupSelectorSpring.x.interpolate((x) => `${100 * x}%`)
            }}
          >
            <GroupSelectorButton
              text={selectedGroup ? selectedGroup.name : i18n.common.all}
              onClick={() => {
                setShowGroupSelector(!showGroupSelector)
              }}
              icon={showGroupSelector ? faAngleUp : faAngleDown}
              iconRight
            />
            <GroupSelector
              groupIdOrAll={groupIdOrAll}
              selectedGroup={selectedGroup}
              changeGroup={changeGroup}
            />
          </GroupSelectorWrapper>
          <Gap size={'XL'} />
          <Tabs tabs={tabs} mobile />

          <FullHeightContentArea
            opaque={false}
            paddingVertical={'s'}
            paddingHorizontal={'zero'}
          >
            <Switch>
              <Route
                exact
                path="/units/:unitId/attendance/:groupId/coming"
                render={() => (
                  <AttendanceComingPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route
                exact
                path="/units/:unitId/attendance/:groupId/present"
                render={() => (
                  <AttendancePresentPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route
                exact
                path="/units/:unitId/attendance/:groupId/departed"
                render={() => (
                  <AttendanceDepartedPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route
                exact
                path="/units/:unitId/attendance/:groupId/absent"
                render={() => (
                  <AttendanceAbsentPage
                    attendanceResponse={attendanceResponse}
                  />
                )}
              />
              <Route path="/" component={RedirectToUnitPage} />
            </Switch>
          </FullHeightContentArea>
        </Fragment>
      )}
    </Fragment>
  )
})

const Name = styled.div`
  background: ${colors.greyscale.white};
`

const GroupSelectorButton = styled(InlineButton)`
  width: 100%;
  border: none;
  font-family: Montserrat, sans-serif;
  font-size: 20px;
  height: 48px;
  flex-shrink: 0;
`

const GroupSelectorWrapper = animated(styled.div`
  box-shadow: 0px 2px 6px 0px ${colors.greyscale.lighter};
  position: absolute;
  z-index: 1;
  display: flex;
  background-color: ${colors.greyscale.white};
  flex-direction: column;
  overflow-y: hidden;
  min-height: 48px;
`)

const NoMarginTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
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

const FullHeightContentArea = styled(ContentArea)`
  height: 100%;
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
