// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { defaultMargins } from 'lib-components/white-space'
import { faLockAlt } from 'lib-icons'

import { useHolidayPeriods } from '../holiday-periods/state'
import { useTranslation } from '../localization'

const LabelContainer = styled.span`
  display: inline-flex;
  align-items: center;
  gap: ${defaultMargins.xs};
`

interface Props {
  iconRight?: boolean
}

export default React.memo(function ReportHolidayLabel({ iconRight }: Props) {
  const i18n = useTranslation()
  const { questionnaireAvailable } = useHolidayPeriods()

  if (!questionnaireAvailable) return null

  return questionnaireAvailable === 'with-strong-auth' ? (
    <LabelContainer>
      {!iconRight && <FontAwesomeIcon icon={faLockAlt} size="xs" />}
      {i18n.calendar.newHoliday}
      {iconRight && <FontAwesomeIcon icon={faLockAlt} size="xs" />}
    </LabelContainer>
  ) : (
    <>{i18n.calendar.newHoliday}</>
  )
})
