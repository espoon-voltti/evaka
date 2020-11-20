// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment } from 'react'

import { isFailure, isLoading, isSuccess, Result } from '~api'
import { AttendanceResponse } from '~api/attendances'
import Loader from '~components/shared/atoms/Loader'
import { useTranslation } from '~state/i18n'
import AttendanceList from './AttendanceList'

interface Props {
  attendanceResponse: Result<AttendanceResponse>
}

export default React.memo(function AttendanceDepartedPage({
  attendanceResponse
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Fragment>
      {isFailure(attendanceResponse) && <div>{i18n.common.loadingFailed}</div>}
      {isLoading(attendanceResponse) && <Loader />}
      {isSuccess(attendanceResponse) && (
        <AttendanceList
          attendanceChildren={attendanceResponse.data.children}
          type={'DEPARTED'}
        />
      )}
    </Fragment>
  )
})
