// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faChild, faComments, faPen } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { unitInfoQuery } from '../units/queries'

interface Props {
  groupRoute: string
  groupHasNotes: boolean
  child: AttendanceChild
}

export default React.memo(function ChildButtons({
  groupRoute,
  groupHasNotes,
  child
}: Props) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()

  const { unitId } = useRouteParams(['unitId'])
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
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
            to={`${groupRoute}/child-attendance/${child.id}/new-message`}
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
          to={`${groupRoute}/child-attendance/${child.id}/note`}
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
          to={`${groupRoute}/child-attendance/${child.id}/info`}
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
