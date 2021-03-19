// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { AttendanceChild } from '../../api/attendances'
import { WideLinkButton } from '../../components/mobile/components'

interface Props {
  unitId: string
  child: AttendanceChild
  groupIdOrAll: string | 'all'
}

export default React.memo(function AttendanceChildComing({
  unitId,
  child,
  groupIdOrAll
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Fragment>
      <FixedSpaceColumn>
        <WideLinkButton
          $primary
          data-qa="mark-present-link"
          to={`/units/${unitId}/groups/${groupIdOrAll}/childattendance/${child.id}/markpresent`}
        >
          {i18n.attendances.actions.markPresent}
        </WideLinkButton>

        <WideLinkButton
          data-qa="mark-absent-link"
          to={`/units/${unitId}/groups/${groupIdOrAll}/childattendance/${child.id}/markabsent`}
        >
          {i18n.attendances.actions.markAbsent}
        </WideLinkButton>
      </FixedSpaceColumn>
      <Gap size={'L'} />
    </Fragment>
  )
})
