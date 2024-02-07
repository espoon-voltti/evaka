// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import {
  AttendanceChild,
  AttendanceTimes
} from 'lib-common/generated/api-types/attendance'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../common/i18n'
import { WideLinkButton } from '../../pairing/components'

interface Props {
  child: AttendanceChild
  attendances: AttendanceTimes[]
  groupRoute: string
}

export default React.memo(function AttendanceChildComing({
  child,
  attendances,
  groupRoute
}: Props) {
  const { i18n } = useTranslation()

  const hasBeenPresentToday = attendances.some(
    (a) =>
      a.arrived.toLocalDate().isToday() ||
      a.departed == null ||
      a.departed.toLocalDate().isToday()
  )

  return (
    <Fragment>
      <FixedSpaceColumn>
        <WideLinkButton
          $primary
          data-qa="mark-present-link"
          to={`${groupRoute}/child-attendance/${child.id}/mark-present`}
        >
          {i18n.attendances.actions.markPresent}
        </WideLinkButton>

        {!hasBeenPresentToday && (
          <WideLinkButton
            data-qa="mark-absent-link"
            to={`${groupRoute}/child-attendance/${child.id}/mark-absent`}
          >
            {i18n.attendances.actions.markAbsent}
          </WideLinkButton>
        )}
      </FixedSpaceColumn>
      <Gap size="s" />
    </Fragment>
  )
})
