// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'
import LocalDate from 'lib-common/local-date'
import Button from 'lib-components/atoms/buttons/Button'
import ModalBackground from 'lib-components/molecules/modals/ModalBackground'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faCalendarPlus, faTreePalm, faUserMinus } from 'lib-icons'
import { useTranslation } from '../localization'

interface Props {
  close: () => void
  openReservations: () => void
  openAbsences: (initialDate: LocalDate | undefined) => void
  openHolidays: () => void
  isHolidayFormActive: boolean
}

export default React.memo(function ActionPickerModal({
  close,
  openReservations,
  openAbsences,
  openHolidays,
  isHolidayFormActive
}: Props) {
  const i18n = useTranslation()
  const onCreateAbsences = useCallback(
    () => openAbsences(undefined),
    [openAbsences]
  )

  return (
    <ModalBackground onClick={close}>
      <Container>
        {isHolidayFormActive && (
          <Action onClick={openHolidays} data-qa="calendar-action-holidays">
            {i18n.calendar.newHoliday}
            <IconBackground>
              <FontAwesomeIcon icon={faTreePalm} size="1x" />
            </IconBackground>
          </Action>
        )}
        <Gap size="s" />
        <Action onClick={onCreateAbsences} data-qa="calendar-action-absences">
          {i18n.calendar.newAbsence}
          <IconBackground>
            <FontAwesomeIcon icon={faUserMinus} size="1x" />
          </IconBackground>
        </Action>
        <Gap size="s" />
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
  )
})

const Container = styled.div`
  position: fixed;
  bottom: calc(46px + ${defaultMargins.L});
  right: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
`

const Action = styled(Button)`
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
