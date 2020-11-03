// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useState } from 'react'
import {
  Redirect,
  Route,
  Switch,
  useLocation,
  useParams
} from 'react-router-dom'
import MetaTags from 'react-meta-tags'
import styled from 'styled-components'

import { useTranslation } from '~state/i18n'
import {
  ChildInGroup,
  getChildrenInGroup,
  getDaycare,
  getDaycareAttendances,
  getUnitData,
  UnitData,
  UnitResponse
} from '~api/unit'
import Tabs from '~components/shared/molecules/Tabs'
import AttendanceGroupSelectorPage from './AttendanceGroupSelectorPage'
import AttendanceComingPage from './AttendanceComingPage'
import AttendancePresentPage from './AttendancePresentPage'
import { isSuccess, Loading, Result } from '~api'
import LocalDate from '@evaka/lib-common/src/local-date'
import Title from '~components/shared/atoms/Title'
import AttendanceDepartedPage from './AttendanceDepartedPage'
import AttendanceAbsentPage from './AttendanceAbsentPage'
import { ContentArea } from '~components/shared/layout/Container'
import Button from '~components/shared/atoms/buttons/Button'
import { DaycareGroup } from '~types/unit'

const Name = styled.div`
  background: white;
`

const NoMarginTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
`

export const WideButton = styled(Button)`
  width: 100%;
`

const Bold = styled.div`
  font-weight: 600;
`

export default React.memo(function AttendancePageWrapper() {
  const { i18n } = useTranslation()
  const { id, groupid } = useParams<{ id: string; groupid: string }>()
  const location = useLocation()

  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading())
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading())
  const [groupAttendances, setGoupAttendances] = useState<
    Result<ChildInGroup[]>
  >(Loading())

  useEffect(() => {
    void getUnitData(id, LocalDate.today(), LocalDate.today()).then(setUnitData)
    void getDaycare(id).then(setUnit)
    groupid === 'all'
      ? void getDaycareAttendances(id).then(setGoupAttendances)
      : void getChildrenInGroup(groupid).then(setGoupAttendances)
  }, [location])

  useEffect(() => {
    void getUnitData(id, LocalDate.today(), LocalDate.today()).then(setUnitData)
    void getDaycare(id).then(setUnit)
    groupid === 'all'
      ? void getDaycareAttendances(id).then(setGoupAttendances)
      : void getChildrenInGroup(groupid).then(setGoupAttendances)
  }, [])

  const totalAttendances = isSuccess(groupAttendances)
    ? groupAttendances.data.length
    : 0
  const totalComing = isSuccess(groupAttendances)
    ? groupAttendances.data.filter(
        (attendance) => attendance.status === 'COMING'
      ).length
    : 0
  const totalPresent = isSuccess(groupAttendances)
    ? groupAttendances.data.filter(
        (attendance) => attendance.status === 'PRESENT'
      ).length
    : 0
  const totalDeparted = isSuccess(groupAttendances)
    ? groupAttendances.data.filter(
        (attendance) => attendance.status === 'DEPARTED'
      ).length
    : 0
  const totalAbsent = isSuccess(groupAttendances)
    ? groupAttendances.data.filter(
        (attendance) => attendance.status === 'ABSENT'
      ).length
    : 0

  const tabs = [
    {
      id: 'coming',
      link: `/units/${id}/attendance/${groupid}/coming`,
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
      link: `/units/${id}/attendance/${groupid}/present`,
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
      link: `/units/${id}/attendance/${groupid}/departed`,
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
      link: `/units/${id}/attendance/${groupid}/absent`,
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
    return <Redirect to={`/units/${id}/groupselector`} />
  }

  const selectedGroup =
    isSuccess(unitData) &&
    groupid !== 'all' &&
    unitData.data.groups.find((elem: DaycareGroup) => elem.id === groupid)

  return (
    <Fragment>
      <MetaTags>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </MetaTags>
      {isSuccess(unitData) && isSuccess(unit) && (
        <Fragment>
          <Name>
            <NoMarginTitle size={1} centered smaller bold>
              {unit.data.daycare.name}
            </NoMarginTitle>
          </Name>
          <Tabs tabs={tabs} mobile />

          <ContentArea
            opaque={false}
            paddingVertical={'s'}
            paddingHorozontal={'s'}
          >
            <a href={`/units/${id}/groupselector`}>
              <WideButton
                primary
                text={
                  selectedGroup
                    ? selectedGroup.name
                    : groupid === 'all'
                    ? i18n.common.all
                    : i18n.attendances.groupSelectError
                }
              />
            </a>
            <Switch>
              <Route
                exact
                path="/units/:id/attendance/groups"
                component={AttendanceGroupSelectorPage}
              />
              <Route
                exact
                path="/units/:id/attendance/:groupid/coming"
                render={() => (
                  <AttendanceComingPage groupAttendances={groupAttendances} />
                )}
              />
              <Route
                exact
                path="/units/:id/attendance/:groupid/present"
                render={() => (
                  <AttendancePresentPage groupAttendances={groupAttendances} />
                )}
              />
              <Route
                exact
                path="/units/:id/attendance/:groupid/departed"
                render={() => (
                  <AttendanceDepartedPage groupAttendances={groupAttendances} />
                )}
              />
              <Route
                exact
                path="/units/:id/attendance/:groupid/absent"
                render={() => (
                  <AttendanceAbsentPage groupAttendances={groupAttendances} />
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
