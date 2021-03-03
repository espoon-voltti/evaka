// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment, useContext } from 'react'

import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'

import {
  AttendanceChild,
  getDaycareAttendances,
  returnToPresent
} from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import { useTranslation } from '../../state/i18n'
import Absences from './Absences'
import { getTimeString } from './AttendanceChildPage'
import {
  ArrivalTime,
  CustomHorizontalLine,
  InlineWideAsyncButton
} from './components'

interface Props {
  child: AttendanceChild
  unitId: string
}

export default React.memo(function AttendanceChildDeparted({
  child,
  unitId
}: Props) {
  const { i18n } = useTranslation()

  const { setAttendanceResponse } = useContext(AttendanceUIContext)

  function returnToPresentCall() {
    return returnToPresent(unitId, child.id)
  }

  return (
    <Fragment>
      <FixedSpaceColumn>
        <ArrivalTime>
          <span>{i18n.attendances.arrivalTime}</span>
          <span>
            {child.attendance?.arrived
              ? getTimeString(child.attendance.arrived)
              : 'xx:xx'}
          </span>
        </ArrivalTime>
        <ArrivalTime>
          <span>{i18n.attendances.departureTime}</span>
          <span>
            {child.attendance?.departed
              ? getTimeString(child.attendance.departed)
              : 'xx:xx'}
          </span>
        </ArrivalTime>
      </FixedSpaceColumn>
      <CustomHorizontalLine />

      {child.absences.length > 0 && (
        <Fragment>
          <Absences attendanceChild={child} />
        </Fragment>
      )}

      <InlineWideAsyncButton
        text={i18n.attendances.actions.returnToPresent}
        onClick={() => returnToPresentCall()}
        onSuccess={async () => {
          await getDaycareAttendances(unitId).then(setAttendanceResponse)
        }}
        data-qa="delete-attendance"
      />
    </Fragment>
  )
})
