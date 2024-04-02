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

import { routes } from '../../App'
import { useTranslation } from '../../common/i18n'
import { UnitOrGroup } from '../../common/unit-or-group'

interface Props {
  unitOrGroup: UnitOrGroup
  child: AttendanceChild
}

export default React.memo(function AttendanceChildDeparted({
  unitOrGroup,
  child
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  return (
    <ReturnToPresentButton
      icon={faArrowRotateLeft}
      text={i18n.attendances.actions.returnToPresent}
      onClick={() => navigate(routes.markPresent(unitOrGroup, child.id).value)}
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
