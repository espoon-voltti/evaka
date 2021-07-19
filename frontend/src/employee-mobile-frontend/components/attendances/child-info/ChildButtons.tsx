{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import { Link } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { faComments, faChild, faPen } from 'lib-icons'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import {
  AttendanceChild,
  DailyNote
} from 'employee-mobile-frontend/api/attendances'
import { useTranslation } from 'employee-mobile-frontend/state/i18n'

interface Props {
  unitId: UUID
  groupId: UUID
  child: AttendanceChild
  groupNote: DailyNote | null | undefined
}

export default React.memo(function ChildButtons({
  unitId,
  groupId,
  child,
  groupNote
}: Props) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const noteFound = child.dailyNote !== null || !!groupNote
  return (
    <IconWrapper>
      <FixedSpaceRow
        spacing={'52px'}
        fullWidth
        maxWidth={'56px'}
        justifyContent={'center'}
      >
        <RoundIcon
          content={faComments}
          color={colors.greyscale.lighter}
          size="XL"
          label={i18n.common.messages}
        />
        <Link
          to={`/units/${unitId}/groups/${groupId}/childattendance/${child.id}/note`}
          data-qa={'link-child-daycare-daily-note'}
        >
          <RoundIcon
            content={faPen}
            color={colors.main.primary}
            size="XL"
            label={i18n.common.dailyNotes}
            bubble={noteFound}
          />
        </Link>
        <Link
          to={`/units/${unitId}/groups/${groupId}/childattendance/${child.id}/pin`}
          data-qa={'link-child-sensitive-info'}
        >
          <RoundIcon
            content={faChild}
            color={colors.main.primary}
            size="XL"
            label={i18n.common.information}
          />
        </Link>
      </FixedSpaceRow>
    </IconWrapper>
  )
})

const IconWrapper = styled.div`
  display: flex;
  justify-content: center;
  margin-top: 36px;
`
