// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { useTranslation } from '../../common/i18n'
import { WideLinkButton } from '../../pairing/components'

interface Props {
  child: AttendanceChild
  groupRoute: string
}

export default React.memo(function AttendanceChildPresent({
  child,
  groupRoute
}: Props) {
  const { i18n } = useTranslation()

  return (
    <FixedSpaceColumn>
      <WideLinkButton
        $primary
        data-qa="mark-departed-link"
        to={`${groupRoute}/child-attendance/${child.id}/mark-departed`}
      >
        {i18n.attendances.actions.markDeparted}
      </WideLinkButton>
    </FixedSpaceColumn>
  )
})
