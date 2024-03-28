// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'

import { routes } from '../../App'
import { useTranslation } from '../../common/i18n'
import { SelectedGroupId } from '../../common/selected-group'
import { WideLinkButton } from '../../pairing/components'

interface Props {
  selectedGroupId: SelectedGroupId
  child: AttendanceChild
}

export default React.memo(function AttendanceChildPresent({
  selectedGroupId,
  child
}: Props) {
  const { i18n } = useTranslation()

  return (
    <FixedSpaceColumn>
      <WideLinkButton
        $primary
        data-qa="mark-departed-link"
        to={routes.markDeparted(selectedGroupId, child.id).value}
      >
        {i18n.attendances.actions.markDeparted}
      </WideLinkButton>
    </FixedSpaceColumn>
  )
})
