// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Child } from 'lib-common/generated/api-types/attendance'

import { returnToComing } from '../../../../api/attendances'
import { ChildAttendanceContext } from '../../../../state/child-attendance'
import { useTranslation } from '../../../../state/i18n'
import { InlineWideAsyncButton } from '../../components'

interface Props {
  child: Child
  unitId: string
}

export default React.memo(function AttendanceChildAbsent({
  child,
  unitId
}: Props) {
  const { i18n } = useTranslation()
  const { reloadAttendances } = useContext(ChildAttendanceContext)

  function returnToComingCall() {
    return returnToComing(unitId, child.id)
  }

  return (
    <InlineWideAsyncButton
      text={i18n.attendances.actions.returnToComing}
      onClick={() => returnToComingCall()}
      onSuccess={reloadAttendances}
      data-qa="delete-attendance"
    />
  )
})
