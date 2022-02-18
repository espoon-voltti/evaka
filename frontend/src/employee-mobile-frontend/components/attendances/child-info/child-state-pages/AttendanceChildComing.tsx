// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { Child } from 'lib-common/generated/api-types/attendance'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../../state/i18n'
import { WideLinkButton } from '../../../mobile/components'

interface Props {
  unitId: string
  child: Child
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
          to={`/units/${unitId}/groups/${groupIdOrAll}/child-attendance/${child.id}/mark-present`}
        >
          {i18n.attendances.actions.markPresent}
        </WideLinkButton>

        <WideLinkButton
          data-qa="mark-absent-link"
          to={`/units/${unitId}/groups/${groupIdOrAll}/child-attendance/${child.id}/mark-absent`}
        >
          {i18n.attendances.actions.markAbsent}
        </WideLinkButton>
      </FixedSpaceColumn>
      <Gap size="s" />
    </Fragment>
  )
})
