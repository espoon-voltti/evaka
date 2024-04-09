// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { routes } from '../../App'
import { useTranslation } from '../../common/i18n'
import { UnitOrGroup } from '../../common/unit-or-group'
import { WideLinkButton } from '../../pairing/components'

interface Props {
  unitOrGroup: UnitOrGroup
  child: AttendanceChild
}

export default React.memo(function AttendanceChildPresent({
  unitOrGroup,
  child
}: Props) {
  const { i18n } = useTranslation()

  return (
    <FixedSpaceColumn>
      <WideLinkButton
        $primary
        data-qa="mark-departed-link"
        to={routes.markDeparted(unitOrGroup, child.id).value}
      >
        {i18n.attendances.actions.markDeparted}
      </WideLinkButton>
    </FixedSpaceColumn>
  )
})
