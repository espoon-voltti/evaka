// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'

import {
  AttendanceChild,
  childDeparts,
  getDaycareAttendances,
  returnToComing
} from '~api/attendances'
import InputField from '~components/shared/atoms/form/InputField'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import { AttendanceUIContext } from '~state/attendance-ui'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'
import { getCurrentTime, getTimeString } from './AttendanceChildPage'
import {
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

  const { filterAndSetAttendanceResponse } = useContext(AttendanceUIContext)

  useEffect(() => {
    void getDaycareAttendances(unitId).then((res) =>
      filterAndSetAttendanceResponse(res, groupIdOrAll)
    )
    return history.listen((location) => {
      if (location.pathname.includes('/depart')) {
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
              onChange={setTime}
              value={time}
              width="s"
              type="time"
              data-qa="set-time"
            />
          </FlexLabel>

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
          />
        </FixedSpaceColumn>
      )}
    </Fragment>
  )
})
