// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { useContext } from 'react'
import { useHistory } from 'react-router-dom'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  AttendanceChild,
  getDaycareAttendances,
  returnToComing
} from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import { useTranslation } from '../../state/i18n'
import { InlineWideAsyncButton } from './components'
import { WideLinkButton } from '../../components/mobile/components'

interface Props {
  child: AttendanceChild
  unitId: string
  groupIdOrAll: string | 'all'
}

export default React.memo(function AttendanceChildPresent({
  child,
  unitId,
  groupIdOrAll
}: Props) {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { setAttendanceResponse } = useContext(AttendanceUIContext)

  function returnToComingCall() {
    return returnToComing(unitId, child.id)
  }

  return (
    <FixedSpaceColumn>
      <WideLinkButton
        $primary
        data-qa="mark-departed-link"
        to={`/units/${unitId}/groups/${groupIdOrAll}/childattendance/${child.id}/markdeparted`}
      >
        {i18n.attendances.actions.markDeparted}
      </WideLinkButton>
      <InlineWideAsyncButton
        text={i18n.attendances.actions.returnToComing}
        onClick={() => returnToComingCall()}
        onSuccess={async () => {
          await getDaycareAttendances(unitId).then(setAttendanceResponse)
          history.goBack()
        }}
        data-qa="return-to-coming-btn"
      />
    </FixedSpaceColumn>
  )
})
