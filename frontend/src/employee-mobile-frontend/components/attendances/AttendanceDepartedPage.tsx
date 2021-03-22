// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment } from 'react'

import { Result } from '@evaka/lib-common/api'
import ErrorSegment from '@evaka/lib-components/atoms/state/ErrorSegment'
import Loader from '@evaka/lib-components/atoms/Loader'

import { AttendanceResponse } from '../../api/attendances'
import AttendanceList from './AttendanceList'

interface Props {
  attendanceResponse: Result<AttendanceResponse>
}

export default React.memo(function AttendanceDepartedPage({
  attendanceResponse
}: Props) {
  return (
    <Fragment>
      {attendanceResponse.isFailure && <ErrorSegment />}
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isSuccess && (
        <AttendanceList
          attendanceChildren={attendanceResponse.value.children}
          type={'DEPARTED'}
        />
      )}
    </Fragment>
  )
})
