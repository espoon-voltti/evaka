// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChild, faComments, faPen } from 'lib-icons'
import React, { useContext } from 'react'
import { Link } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'
import { Child } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { useTranslation } from '../../../state/i18n'
import { UnitContext } from '../../../state/unit'
import { renderResult } from '../../async-rendering'

interface Props {
  unitId: UUID
  groupId: UUID
  child: Child
  hasGroupNote: boolean
}

export default React.memo(function ChildButtons({
  unitId,
  groupId,
  child,
  hasGroupNote
}: Props) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()

  const { unitInfoResponse } = useContext(UnitContext)
  const noteFound =
    child.dailyNote !== null || child.stickyNotes.length > 0 || hasGroupNote
  return renderResult(unitInfoResponse, (unit) => (
    <IconWrapper>
      <FixedSpaceRow
        spacing="52px"
        fullWidth
        maxWidth="56px"
        justifyContent="center"
      >
        {unit.features.includes('MOBILE_MESSAGING') ? (
          <Link
            to={`/units/${unitId}/groups/${groupId}/messages/${child.id}/new-message`}
            data-qa="link-new-message"
          >
            <RoundIcon
              content={faComments}
              color={colors.main.primary}
              size="XL"
              label={i18n.childButtons.newMessage}
            />
          </Link>
        ) : (
          <></>
        )}
        <Link
          to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/note`}
          data-qa="link-child-daycare-daily-note"
        >
          <RoundIcon
            content={faPen}
            color={colors.main.primary}
            size="XL"
            label={i18n.common.dailyNotes}
            bubble={noteFound}
            data-qa="daily-note-icon"
          />
        </Link>
        <Link
          to={`/units/${unitId}/groups/${groupId}/child-attendance/${child.id}/info`}
          data-qa="link-child-sensitive-info"
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
  ))
})

const IconWrapper = styled.div`
  display: flex;
  justify-content: center;
  margin-top: 36px;
`
