// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useNavigate } from 'react-router-dom'

import { Child } from 'lib-common/generated/api-types/attendance'

import { returnToPresent } from '../../../../api/attendances'
import { ChildAttendanceContext } from '../../../../state/child-attendance'
import { useTranslation } from '../../../../state/i18n'
import { InlineWideAsyncButton } from '../../components'

interface Props {
  child: Child
  unitId: string
}

export default React.memo(function AttendanceChildDeparted({
  child,
  unitId
}: Props) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const { reloadAttendances } = useContext(ChildAttendanceContext)

  function returnToPresentCall() {
    return returnToPresent(unitId, child.id)
  }

  return (
    <InlineWideAsyncButton
      text={i18n.attendances.actions.returnToPresent}
      onClick={() => returnToPresentCall()}
      onSuccess={() => {
        reloadAttendances()
        navigate(-1)
      }}
      data-qa="return-to-present-btn"
    />
  )
})
