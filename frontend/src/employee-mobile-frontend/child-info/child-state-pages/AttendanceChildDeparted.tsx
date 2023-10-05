// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faArrowRotateLeft } from 'Icons'
import React from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../common/i18n'

interface Props {
  child: AttendanceChild
  groupRoute: string
}

export default React.memo(function AttendanceChildDeparted({
  child,
  groupRoute
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <ReturnToPresentButton
      icon={faArrowRotateLeft}
      text={i18n.attendances.actions.returnToPresent}
      onClick={() =>
        navigate(`${groupRoute}/child-attendance/${child.id}/mark-present`)
      }
      data-qa="return-to-present-btn"
    />
  )
})

export const ReturnToPresentButton = styled(InlineButton)`
  color: ${colors.main.m2};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  width: calc(100vw - 50px);
  display: flex;
  justify-content: center;
`
