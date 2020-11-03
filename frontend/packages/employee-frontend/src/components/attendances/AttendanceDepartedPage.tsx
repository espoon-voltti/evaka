// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment } from 'react'

import { isFailure, isLoading, Result } from '~api'
import { ChildInGroup } from '~api/unit'
import Loader from '~components/shared/atoms/Loader'
import { useTranslation } from '~state/i18n'
import AttendanceList from './AttendanceList'

interface Props {
  groupAttendances: Result<ChildInGroup[]>
}

export default React.memo(function AttendanceDepartedPage({
  groupAttendances
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Fragment>
      {isFailure(groupAttendances) && <div>{i18n.common.loadingFailed}</div>}
      {isLoading(groupAttendances) ? (
        <Loader />
      ) : (
        <AttendanceList groupAttendances={groupAttendances} type={'DEPARTED'} />
      )}
    </Fragment>
  )
})
