// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import ModalAccessibilityWrapper from 'citizen-frontend/ModalAccessibilityWrapper'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ModalBackground from 'lib-components/molecules/modals/ModalBackground'
import { defaultMargins } from 'lib-components/white-space'
import { faCalendarPlus, faTreePalm, faUserMinus } from 'lib-icons'

import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { mobileBottomNavHeight } from '../navigation/const'

import ReportHolidayLabel from './ReportHolidayLabel'
import { activeQuestionnaireQuery } from './queries'
import { isQuestionnaireAvailable } from './utils'

interface Props {
  close: () => void
  openReservations: () => void
  openAbsences: (initialDate: LocalDate | undefined) => void
  openHolidays: () => void
}

export default React.memo(function ActionPickerModal({
  close,
  openReservations,
  openAbsences,
  openHolidays
}: Props) {
  const i18n = useTranslation()
  const onCreateAbsences = useCallback(
    () => openAbsences(undefined),
    [openAbsences]
  )
  const questionnaireAvailable = isQuestionnaireAvailable(
    useQueryResult(activeQuestionnaireQuery()),
    useUser()
  )

  return (
    <ModalAccessibilityWrapper>
      <ModalBackground onClick={close}>
        <Container>
          {questionnaireAvailable && (
            <Action onClick={openHolidays} data-qa="calendar-action-holidays">
              <ReportHolidayLabel
                questionnaireAvailable={questionnaireAvailable}
              />
              <IconBackground>
                <FontAwesomeIcon icon={faTreePalm} size="1x" />
              </IconBackground>
            </Action>
          )}
          <Action onClick={onCreateAbsences} data-qa="calendar-action-absences">
            {i18n.calendar.newAbsence}
            <IconBackground>
              <FontAwesomeIcon icon={faUserMinus} size="1x" />
            </IconBackground>
          </Action>
          <Action
            onClick={openReservations}
            data-qa="calendar-action-reservations"
          >
            {i18n.calendar.newReservationBtn}
            <IconBackground>
              <FontAwesomeIcon icon={faCalendarPlus} size="1x" />
            </IconBackground>
          </Action>
        </Container>
      </ModalBackground>
    </ModalAccessibilityWrapper>
  )
})

const Container = styled.div`
  position: fixed;
  bottom: calc(46px + ${defaultMargins.L} + ${mobileBottomNavHeight}px);
  right: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  gap: ${defaultMargins.s};
`

const Action = styled(LegacyButton)`
  border: none;
  background: none;
  color: ${(p) => p.theme.colors.grayscale.g0};
  padding: 0;
  min-height: 0;
`

const IconBackground = styled.div`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 34px;
  height: 34px;
  color: ${(p) => p.theme.colors.main.m2};
  background: ${(p) => p.theme.colors.grayscale.g0};
  margin-left: ${defaultMargins.s};
  border-radius: 50%;
`
