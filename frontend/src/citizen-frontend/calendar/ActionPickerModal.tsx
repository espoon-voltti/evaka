// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import type LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { useThrottledEventHandler } from 'lib-components/atoms/buttons/button-commons'
import { renderBaseButton } from 'lib-components/atoms/buttons/button-visuals'
import { zoomedMobileMax } from 'lib-components/breakpoints'
import ModalBackground from 'lib-components/molecules/modals/ModalBackground'
import type { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { faComment } from 'lib-icons'
import { faCalendarPlus, faTreePalm, faUserMinus } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
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
  openDiscussionReservations: (initialDate: LocalDate | undefined) => void
  isDiscussionActionVisible: boolean
}

export default React.memo(function ActionPickerModal({
  close,
  openReservations,
  openAbsences,
  openHolidays,
  openDiscussionReservations,
  isDiscussionActionVisible
}: Props) {
  const i18n = useTranslation()
  const onCreateAbsences = useCallback(
    () => openAbsences(undefined),
    [openAbsences]
  )
  const onOpenDiscussionReservation = useCallback(
    () => openDiscussionReservations(undefined),
    [openDiscussionReservations]
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
            <Action
              onClick={openHolidays}
              data-qa="calendar-action-holidays"
              icon={faTreePalm}
            >
              <ReportHolidayLabel
                questionnaireAvailable={questionnaireAvailable}
              />
            </Action>
          )}
          {featureFlags.discussionReservations && isDiscussionActionVisible && (
            <Action
              onClick={onOpenDiscussionReservation}
              data-qa="calendar-action-discussions"
              icon={faComment}
            >
              {i18n.calendar.discussionTimeReservation.surveyModalButtonText}
            </Action>
          )}
          <Action
            onClick={onCreateAbsences}
            data-qa="calendar-action-absences"
            icon={faUserMinus}
          >
            {i18n.calendar.newAbsence}
          </Action>
          <Action
            onClick={openReservations}
            data-qa="calendar-action-reservations"
            icon={faCalendarPlus}
          >
            {i18n.calendar.newReservationBtn}
          </Action>
        </Container>
      </ModalBackground>
    </ModalAccessibilityWrapper>
  )
})

const baseBottomValue = 46

const Container = styled.div`
  position: fixed;
  bottom: calc(
    ${baseBottomValue}px + ${defaultMargins.L} + ${mobileBottomNavHeight}px
  );
  right: ${defaultMargins.s};
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  gap: ${defaultMargins.s};
  @media (max-width: ${zoomedMobileMax}) {
    align-items: flex-start;
    left: 0;
    right: 0;
    padding: 0 ${defaultMargins.xs};
    row-gap: clamp(4px, 1.5vh, ${defaultMargins.xs});
    bottom: calc(
      ${baseBottomValue}px + ${defaultMargins.s} + ${mobileBottomNavHeight}px
    );
    button {
      white-space: normal;
      text-align: left;
      min-height: ${defaultMargins.L};
    }
  }
`

const ActionButton = React.memo(function ActionButton({
  onClick,
  icon,
  children,
  ...props
}: {
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void
  icon: IconDefinition
  children: React.ReactNode
} & BaseProps) {
  const handleOnClick = useThrottledEventHandler(onClick)
  return renderBaseButton({ ...props, text: '' }, handleOnClick, () => (
    <>
      {children}
      <IconBackground>
        <FontAwesomeIcon icon={icon} size="1x" />
      </IconBackground>
    </>
  ))
})

const Action = styled(ActionButton)`
  border: none;
  background: none;
  color: ${(p) => p.theme.colors.grayscale.g0};
  padding: 0;
  min-height: 0;
  svg {
    margin-right: 0;
  }
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
  @media (max-width: ${zoomedMobileMax}) {
    display: none;
  }
`
