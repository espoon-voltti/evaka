// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment, useContext } from 'react'

import {
  AttendanceChild,
  getDaycareAttendances,
  returnToComing
} from '~api/attendances'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { AttendanceUIContext } from '~state/attendance-ui'
import { useTranslation } from '~state/i18n'
import Absences from './Absences'
import { InlineWideAsyncButton } from './components'

interface Props {
  child: AttendanceChild
  unitId: string
  groupId: string | 'all'
}

export default React.memo(function AttendanceChildAbsent({
  child,
  unitId,
  groupId: groupIdOrAll
}: Props) {
  const { i18n } = useTranslation()
  const { filterAndSetAttendanceResponse } = useContext(AttendanceUIContext)

  function returnToComingCall() {
    return returnToComing(unitId, child.id)
  }

  return (
    <Fragment>
      <FixedSpaceColumn>
        <Absences attendanceChild={child} />
        <InlineWideAsyncButton
          text={i18n.attendances.actions.returnToComing}
          onClick={() => returnToComingCall()}
          onSuccess={async () => {
            await getDaycareAttendances(unitId).then((res) =>
              filterAndSetAttendanceResponse(res, groupIdOrAll)
            )
          }}
          data-qa="delete-attendance"
        />
      </FixedSpaceColumn>
    </Fragment>
  )
})
