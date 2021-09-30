// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useHistory } from 'react-router-dom'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { returnToComing } from '../../api/attendances'
import { Child } from 'lib-common/generated/api-types/attendance'
import { ChildAttendanceContext } from '../../state/child-attendance'
import { useTranslation } from '../../state/i18n'
import { InlineWideAsyncButton } from './components'
import { WideLinkButton } from '../mobile/components'

interface Props {
  child: Child
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

  const { reloadAttendances } = useContext(ChildAttendanceContext)

  function returnToComingCall() {
    return returnToComing(unitId, child.id)
  }

  return (
    <FixedSpaceColumn>
      <WideLinkButton
        $primary
        data-qa="mark-departed-link"
        to={`/units/${unitId}/groups/${groupIdOrAll}/child-attendance/${child.id}/mark-departed`}
      >
        {i18n.attendances.actions.markDeparted}
      </WideLinkButton>
      <InlineWideAsyncButton
        text={i18n.attendances.actions.returnToComing}
        onClick={() => returnToComingCall()}
        onSuccess={() => {
          reloadAttendances()
          history.goBack()
        }}
        data-qa="return-to-coming-btn"
      />
    </FixedSpaceColumn>
  )
})
