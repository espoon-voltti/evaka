// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment, useContext } from 'react'

import {
  AttendanceChild,
  getDaycareAttendances,
  returnToPresent
} from '../../api/attendances'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { Gap } from '@evaka/lib-components/src/white-space'
import { AttendanceUIContext } from '../../state/attendance-ui'
import { useTranslation } from '../../state/i18n'
import Absences from './Absences'
import { getTimeString } from './AttendanceChildPage'
import { FlexLabel, InlineWideAsyncButton } from './components'

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
            data-qa="set-time"
            readonly
          />
        </FlexLabel>

        <FlexLabel>
          <span>{i18n.attendances.departureTime}</span>
          <InputField
            onChange={undefined}
            value={
              child.attendance?.departed
                ? getTimeString(child.attendance.departed)
                : 'xx:xx'
            }
            width="s"
            type="time"
            data-qa="set-time"
            readonly
          />
        </FlexLabel>
      </FixedSpaceColumn>

      {child.absences.length > 0 && (
        <Fragment>
          <Gap size={'L'} />
          <Absences attendanceChild={child} />
        </Fragment>
      )}

      <Gap size={'m'} />
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
