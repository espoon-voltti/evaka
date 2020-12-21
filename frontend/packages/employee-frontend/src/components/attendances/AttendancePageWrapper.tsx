// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'

import {
  Link,
  Redirect,
  Route,
  Switch,
  useLocation,
  useParams
} from 'react-router-dom'
import MetaTags from 'react-meta-tags'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'

import { useTranslation } from '~state/i18n'
import Tabs from '@evaka/lib-components/src/molecules/Tabs'
import AttendanceGroupSelectorPage from './AttendanceGroupSelectorPage'
import AttendanceComingPage from './AttendanceComingPage'
import AttendancePresentPage from './AttendancePresentPage'
import Title from '@evaka/lib-components/src/atoms/Title'
import AttendanceDepartedPage from './AttendanceDepartedPage'
import AttendanceAbsentPage from './AttendanceAbsentPage'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import colors from '@evaka/lib-components/src/colors'
import { faSearch, faTimes } from '@evaka/lib-icons'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { FreeTextSearch } from '~components/common/Filters'
import { useDebounce } from '~utils/useDebounce'
import AttendanceList from './AttendanceList'
import { AttendanceChild, getDaycareAttendances, Group } from '~api/attendances'
import { AttendanceUIContext } from '~state/attendance-ui'
import { WideButton } from './components'

const Name = styled.div`
  background: white;
`

const NoMarginTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: ${colors.blues.primary};
  color: ${colors.greyscale.white};

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
  z-index: 1;
`)

export default React.memo(function AttendancePageWrapper() {
  const { i18n } = useTranslation()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string
  }>()
  const location = useLocation()

  const { attendanceResponse, filterAndSetAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [showSearch, setShowSearch] = useState<boolean>(false)
  const [freeText, setFreeText] = useState<string>('')
  const [searchResults, setSearchResults] = useState<AttendanceChild[]>([])
  const debouncedSearchTerms = useDebounce(freeText, 500)

  useEffect(() => {
    void getDaycareAttendances(unitId).then((res) =>
      filterAndSetAttendanceResponse(res, groupIdOrAll)
    )
  }, [location])

  useEffect(() => {
    if (attendanceResponse.isSuccess) {
      const filteredData = attendanceResponse.value.children.filter(
        (ac) =>
          ac.firstName
            .toLowerCase()
            .includes(debouncedSearchTerms.toLowerCase()) ||
          ac.lastName.toLowerCase().includes(debouncedSearchTerms.toLowerCase())
      )
      setSearchResults(filteredData)
    }
  }, [debouncedSearchTerms])

  const totalAttendances = attendanceResponse
    .map((res) => res.children.length)
    .getOrElse(0)
  const totalComing = attendanceResponse
    .map(
      (res) =>
        res.children.filter((attendance) => attendance.status === 'COMING')
          .length
    )
    .getOrElse(0)
  const totalPresent = attendanceResponse
    .map(
      (res) =>
        res.children.filter((attendance) => attendance.status === 'PRESENT')
          .length
    )
    .getOrElse(0)
  const totalDeparted = attendanceResponse
    .map(
      (res) =>
        res.children.filter((attendance) => attendance.status === 'DEPARTED')
          .length
    )
    .getOrElse(0)
  const totalAbsent = attendanceResponse
    .map(
      (res) =>
        res.children.filter((attendance) => attendance.status === 'ABSENT')
          .length
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

  function RedirectToGroupSelector() {
    return <Redirect to={`/units/${unitId}/groupselector`} />
  }

  const selectedGroup =
    attendanceResponse.isSuccess &&
    groupIdOrAll !== 'all' &&
    attendanceResponse.value.unit.groups.find(
      (elem: Group) => elem.id === groupIdOrAll
    )

  const container = useSpring({ x: showSearch ? 1 : 0 })

  return (
    <Fragment>
      <MetaTags>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </MetaTags>
      {attendanceResponse.isSuccess && (
        <Fragment>
          <SearchBar
            style={{
              height: container.x.interpolate((x) => `${100 * x}vh`)
            }}
          >
            <NoMarginTitle size={1} centered smaller bold>
              {attendanceResponse.value.unit.name}{' '}
              <IconButton
                onClick={() => setShowSearch(!showSearch)}
                icon={faTimes}
              />
            </NoMarginTitle>
            <ContentArea opaque={false} paddingHorozontal={'s'}>
              <FreeTextSearch
                value={freeText}
                setValue={setFreeText}
                placeholder={i18n.attendances.searchPlaceholder}
                background={colors.greyscale.white}
              />
              <AttendanceList attendanceChildren={searchResults} />
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
          <Tabs tabs={tabs} mobile />

          <ContentArea
            opaque={false}
            paddingVertical={'s'}
            paddingHorozontal={'s'}
          >
            <Link to={`/units/${unitId}/groupselector`}>
              <WideButton
                primary
                text={
                  selectedGroup
                    ? selectedGroup.name
                    : groupIdOrAll === 'all'
                    ? i18n.common.all
                    : i18n.attendances.groupSelectError
                }
              />
            </Link>
            <Switch>
              <Route
                exact
                path="/units/:unitId/attendance/groups"
                component={AttendanceGroupSelectorPage}
              />
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
              <Route path="/" component={RedirectToGroupSelector} />
            </Switch>
          </ContentArea>
        </Fragment>
      )}
    </Fragment>
  )
})
