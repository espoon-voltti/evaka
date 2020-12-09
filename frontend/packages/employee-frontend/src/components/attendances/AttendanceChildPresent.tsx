// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { isBefore, parse } from 'date-fns'
import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'

import { Result, Loading } from '~api'
import {
  AttendanceChild,
  childDeparts,
  DepartureInfoResponse,
  getChildDeparture,
  getDaycareAttendances,
  postDeparture,
  returnToComing
} from '~api/attendances'
import InputField from '~components/shared/atoms/form/InputField'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import { Gap } from '~components/shared/layout/white-space'
import { AttendanceUIContext } from '~state/attendance-ui'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { AbsenceType } from '~types/absence'
import AbsenceSelector from './AbsenceSelector'
import { getCurrentTime, getTimeString } from './AttendanceChildPage'
import {
  AbsentFrom,
  BigWideButton,
  FlexLabel,
  InlineWideAsyncButton,
  WideAsyncButton
} from './components'

interface Props {
  child: AttendanceChild
  unitId: UUID
  groupId: UUID | 'all'
}

export default React.memo(function AttendanceChildPresent({
  child,
  unitId,
  groupId: groupIdOrAll
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()

  const [time, setTime] = useState<string>(getCurrentTime())
  const [markDepart, setMarkDepart] = useState<boolean>(false)
  const [timeError, setTimeError] = useState<boolean>(false)
  const [childDepartureInfo, setChildDepartureInfo] = useState<
    Result<DepartureInfoResponse>
  >(Loading.of())

  const { filterAndSetAttendanceResponse } = useContext(AttendanceUIContext)

  useEffect(() => {
    void getDaycareAttendances(unitId).then((res) =>
      filterAndSetAttendanceResponse(res, groupIdOrAll)
    )
    return history.listen((location) => {
      if (location.pathname.includes('/depart')) {
        void getChildDeparture(unitId, child.id, getCurrentTime()).then(
          setChildDepartureInfo
        )
        setTime(getCurrentTime())
        setMarkDepart(true)
      } else {
        setMarkDepart(false)
        void getDaycareAttendances(unitId).then((res) =>
          filterAndSetAttendanceResponse(res, groupIdOrAll)
        )
      }
    })
  }, [])

  function markDeparted() {
    return childDeparts(unitId, child.id, time)
  }

  function returnToComingCall() {
    return returnToComing(unitId, child.id)
  }

  function selectAbsenceType(absenceType: AbsenceType) {
    return postDeparture(unitId, child.id, absenceType, time)
  }

  function updateTime(newTime: string) {
    if (child.attendance !== null) {
      const newDate = parse(newTime, 'HH:mm', new Date())
      if (isBefore(newDate, child.attendance.arrived)) {
        setTimeError(true)
      } else {
        setTimeError(false)
        void getChildDeparture(unitId, child.id, newTime).then(
          setChildDepartureInfo
        )
      }
    }
    setTime(newTime)
  }

  return (
    <Fragment>
      {!markDepart && (
        <FixedSpaceColumn>
          <FlexLabel>
            <span>{i18n.attendances.arrivalTime}</span>
            <InputField
              onChange={undefined}
              value={
                child.attendance?.arrived
                  ? getTimeString(child.attendance.arrived)
                  : 'xx:xx'
              }
              width="s"
              type="time"
              data-qa="arrival-time"
              readonly
            />
          </FlexLabel>

          <BigWideButton
            primary
            text={i18n.attendances.actions.markLeaving}
            onClick={() =>
              history.push(
                `/units/${unitId}/groups/${groupIdOrAll}/childattendance/${child.id}/depart`
              )
            }
          />
          <InlineWideAsyncButton
            text={i18n.attendances.actions.returnToComing}
            onClick={() => returnToComingCall()}
            onSuccess={async () => {
              await getDaycareAttendances(unitId).then((res) =>
                filterAndSetAttendanceResponse(res, groupIdOrAll)
              )
              history.goBack()
            }}
            data-qa="delete-attendance"
          />
        </FixedSpaceColumn>
      )}

      {markDepart && (
        <FixedSpaceColumn>
          <FlexLabel>
            <span>{i18n.attendances.arrivalTime}</span>
            <InputField
              onChange={setTime}
              value={
                child.attendance?.arrived
                  ? getTimeString(child.attendance.arrived)
                  : 'xx:xx'
              }
              width="s"
              type="time"
              data-qa="set-time"
              readonly
            />
          </FlexLabel>

          <FlexLabel>
            <span>{i18n.attendances.departureTime}</span>
            <InputField
              onChange={updateTime}
              value={time}
              width="s"
              type="time"
              info={
                timeError
                  ? { text: i18n.attendances.timeError, status: 'warning' }
                  : undefined
              }
              data-qa="set-time"
            />
          </FlexLabel>

          {childDepartureInfo.isSuccess &&
          childDepartureInfo.value.absentFrom.length > 0 &&
          !timeError ? (
            <Fragment>
              <AbsentFrom
                child={child}
                absentFrom={childDepartureInfo.value.absentFrom}
              />
              <AbsenceSelector
                unitId={unitId}
                groupId={groupIdOrAll}
                selectAbsenceType={selectAbsenceType}
              />
            </Fragment>
          ) : (
            <Fragment>
              {timeError && <Gap size={'s'} />}
              <WideAsyncButton
                primary
                text={i18n.common.confirm}
                onClick={markDeparted}
                onSuccess={async () => {
                  await getDaycareAttendances(unitId).then((res) =>
                    filterAndSetAttendanceResponse(res, groupIdOrAll)
                  )
                  history.goBack()
                }}
                data-qa="mark-departed"
                disabled={timeError}
              />
            </Fragment>
          )}
        </FixedSpaceColumn>
      )}
    </Fragment>
  )
})
