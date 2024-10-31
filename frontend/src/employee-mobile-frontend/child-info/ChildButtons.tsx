// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faChild, faComments, faPen } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { unitInfoQuery } from '../units/queries'

interface Props {
  unitId: UUID
  groupHasNotes: boolean
  child: AttendanceChild
}

export default React.memo(function ChildButtons({
  unitId,
  groupHasNotes,
  child
}: Props) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()

  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId: unitId }))
  const noteFound =
    child.dailyNote !== null || child.stickyNotes.length > 0 || groupHasNotes
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
            to={routes.newChildMessage(unitId, child.id).value}
            data-qa="link-new-message"
          >
            <RoundIcon
              content={faComments}
              color={colors.main.m2}
              size="XL"
              label={i18n.childButtons.newMessage}
            />
          </Link>
        ) : (
          <></>
        )}
        <Link
          to={routes.childNotes(unitId, child.id).value}
          data-qa="link-child-daycare-daily-note"
        >
          <RoundIcon
            content={faPen}
            color={colors.main.m2}
            size="XL"
            label={i18n.common.dailyNotes}
            bubble={noteFound}
            data-qa="daily-note-icon"
          />
        </Link>
        <Link
          to={routes.childSensitiveInfo(unitId, child.id).value}
          data-qa="link-child-sensitive-info"
        >
          <RoundIcon
            content={faChild}
            color={colors.main.m2}
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
