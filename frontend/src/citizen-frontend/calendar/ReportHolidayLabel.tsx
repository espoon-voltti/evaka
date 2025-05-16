// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { defaultMargins } from 'lib-components/white-space'
import { faLockAlt } from 'lib-icons'

import { useTranslation } from '../localization'

import type { QuestionnaireAvailability } from './utils'

const LabelContainer = styled.span`
  display: inline-flex;
  align-items: center;
  gap: ${defaultMargins.xs};
`

interface Props {
  questionnaireAvailable: QuestionnaireAvailability
  iconRight?: boolean
}

export default React.memo(function ReportHolidayLabel({
  questionnaireAvailable,
  iconRight
}: Props) {
  const i18n = useTranslation()
  return questionnaireAvailable === 'with-strong-auth' ? (
    <LabelContainer>
      {!iconRight && <FontAwesomeIcon icon={faLockAlt} size="xs" />}
      {i18n.calendar.newHoliday}
      {iconRight && <FontAwesomeIcon icon={faLockAlt} size="xs" />}
    </LabelContainer>
  ) : questionnaireAvailable ? (
    <>{i18n.calendar.newHoliday}</>
  ) : null
})
