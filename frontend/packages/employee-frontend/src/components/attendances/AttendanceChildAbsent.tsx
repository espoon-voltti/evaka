// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { Fragment } from 'react'

import { AttendanceChild } from '~api/attendances'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import Absences from './Absences'

interface Props {
  child: AttendanceChild
}

export default React.memo(function AttendanceChildAbsent({ child }: Props) {
  return (
    <Fragment>
      <FixedSpaceColumn>
        <Absences attendanceChild={child} />
      </FixedSpaceColumn>
    </Fragment>
  )
})
