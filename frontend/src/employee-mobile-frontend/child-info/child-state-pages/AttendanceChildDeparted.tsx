// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import type { UUID } from 'lib-common/types'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faArrowRotateLeft } from 'lib-icons'

import { routes } from '../../App'
import { useTranslation } from '../../common/i18n'

interface Props {
  unitId: UUID
  child: AttendanceChild
}

export default React.memo(function AttendanceChildDeparted({
  unitId,
  child
}: Props) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()

  return (
    <ReturnToPresentButton
      icon={faArrowRotateLeft}
      text={i18n.attendances.actions.returnToPresent}
      onClick={() =>
        navigate(routes.markPresent(unitId, [child.id], false).value)
      }
      data-qa="return-to-present-btn"
    />
  )
})

export const ReturnToPresentButton = styled(LegacyInlineButton)`
  color: ${colors.main.m2};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  width: calc(100vw - 50px);
  display: flex;
  justify-content: center;
`
