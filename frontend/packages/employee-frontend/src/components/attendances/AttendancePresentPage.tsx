// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment } from 'react'

import { Result } from '~api'
import { AttendanceResponse } from '~api/attendances'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { useTranslation } from '~state/i18n'
import AttendanceList from './AttendanceList'

interface Props {
  attendanceResponse: Result<AttendanceResponse>
}

export default React.memo(function AttendancePresentPage({
  attendanceResponse
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Fragment>
      {attendanceResponse.isFailure && <div>{i18n.common.loadingFailed}</div>}
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isSuccess && (
        <AttendanceList
          attendanceChildren={attendanceResponse.value.children}
          type={'PRESENT'}
        />
      )}
    </Fragment>
  )
})
