// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import { Failure } from 'lib-common/api'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { combine } from 'lib-common/form/types'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { ReservationsResponse } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { SelectionChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import { TabletAndDesktop } from 'lib-components/layout/responsive-layout'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import {
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, Label, Light } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../localization'

import { BottomFooterContainer } from './BottomFooterContainer'
import {
  CalendarModalBackground,
  CalendarModalButtons,
  CalendarModalCloseButton,
  CalendarModalSection
} from './CalendarModal'
import { postReservationsMutation } from './queries'
import RepetitionTimeInputGrid from './reservation-modal/RepetitionTimeInputGrid'
import {
  DayProperties,
  initialState,
  reservationForm,
  resetTimes
} from './reservation-modal/form'

interface Props {
  onClose: () => void
  onSuccess: () => void
  reservationsResponse: ReservationsResponse
  initialStart: LocalDate | null
  initialEnd: LocalDate | null
  holidayPeriods: HolidayPeriod[]
}

export default React.memo(function ReservationModal({
  onClose,
  onSuccess,
  reservationsResponse,
  initialStart,
  initialEnd,
  holidayPeriods
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const {
    children: availableChildren,
    days: calendarDays,
    reservableRange
  } = reservationsResponse

  const dayProperties = useMemo(
    () => new DayProperties(calendarDays, reservableRange),
    [calendarDays, reservableRange]
  )
  const form = useForm(
    reservationForm,
    () =>
      initialState(
        dayProperties,
        availableChildren,
        initialStart,
        initialEnd,
        i18n
      ),
    { ...i18n.validationErrors, ...i18n.calendar.validationErrors },
    {
      onUpdate: (prevState, nextState, form) => {
        const shape = form.shape()
        const prev = combine(
          shape.repetition.validate(prevState.repetition),
          shape.dateRange.validate(prevState.dateRange),
          shape.selectedChildren.validate(prevState.selectedChildren)
        )
        const next = combine(
          shape.repetition.validate(nextState.repetition),
          shape.dateRange.validate(nextState.dateRange),
          shape.selectedChildren.validate(nextState.selectedChildren)
        )

        if (!next.isValid) return nextState
        const [repetition, selectedRange, selectedChildren] = next.value

        if (!prev.isValid) {
          return {
            ...nextState,
            times: resetTimes(dayProperties, undefined, {
              repetition,
              selectedRange,
              selectedChildren
            })
          }
        }

        const [prevRepetition, prevDateRange, prevSelectedChildren] = prev.value
        const dateRangeChanged = !prevDateRange.isEqual(selectedRange)
        const childrenChanged = !isEqual(prevSelectedChildren, selectedChildren)
        if (
          prevRepetition !== repetition ||
          dateRangeChanged ||
          childrenChanged
        ) {
          return {
            ...nextState,
            times: resetTimes(
              dayProperties,
              {
                childrenChanged,
                times: prevState.times
              },
              {
                repetition,
                selectedRange,
                selectedChildren
              }
            )
          }
        }
        return nextState
      }
    }
  )

  const { selectedChildren, repetition, dateRange, times } = useFormFields(form)
  const [showAllErrors, setShowAllErrors] = useState(false)

  const selectedRange = dateRange.isValid() ? dateRange.value() : undefined

  const [saveError, setSaveError] = useState<string | undefined>()
  const showSaveError = useCallback(
    (reason: Failure<unknown>) => {
      if (reason.errorCode === 'NON_RESERVABLE_DAYS') {
        setSaveError(
          i18n.calendar.reservationModal.saveErrors.NON_RESERVABLE_DAYS
        )
      }
    },
    [i18n, setSaveError]
  )

  const duplicateChildInfo = getDuplicateChildInfo(availableChildren, i18n)

  const anyShiftCare = useMemo(
    () => calendarDays.some((d) => d.children.some((c) => c.shiftCare)),
    [calendarDays]
  )

  const overlappingClosedHolidayPeriods = useMemo(
    () =>
      dateRange.isValid()
        ? holidayPeriods
            .filter(
              (hp) =>
                hp.period.overlaps(dateRange.value()) &&
                hp.reservationDeadline.isBefore(LocalDate.todayInHelsinkiTz())
            )
            .map((hp) => hp.period)
        : [],
    [holidayPeriods, dateRange]
  )

  return (
    <ModalAccessibilityWrapper>
      <PlainModal mobileFullScreen margin="auto" data-qa="reservation-modal">
        <CalendarModalBackground>
          <BottomFooterContainer>
            <div>
              <CalendarModalSection>
                <Gap size="L" sizeOnMobile="zero" />
                <ModalHeader
                  headingComponent={(props) => (
                    <H1 noMargin data-qa="title" {...props}>
                      {props.children}
                    </H1>
                  )}
                >
                  {i18n.calendar.reservationModal.title}
                </ModalHeader>
              </CalendarModalSection>

              <Gap size="zero" sizeOnMobile="s" />

              <CalendarModalSection>
                <H2>{i18n.calendar.reservationModal.selectChildren}</H2>
                <Gap size="xs" />
                <FixedSpaceFlexWrap>
                  {availableChildren
                    .filter((child) => child.upcomingPlacementType !== null)
                    .map((child) => (
                      <div key={child.id} data-qa="relevant-child">
                        <SelectionChip
                          key={child.id}
                          text={`${formatFirstName(child)}${
                            duplicateChildInfo[child.id] !== undefined
                              ? ` ${duplicateChildInfo[child.id]}`
                              : ''
                          }`}
                          translate="no"
                          selected={selectedChildren.state.includes(child.id)}
                          onChange={(selected) => {
                            selectedChildren.update((prev) =>
                              selected
                                ? [...prev, child.id]
                                : prev.filter((id) => id !== child.id)
                            )
                          }}
                          data-qa={`child-${child.id}`}
                        />
                      </div>
                    ))}
                </FixedSpaceFlexWrap>
              </CalendarModalSection>

              <Gap size="xxs" sizeOnMobile="s" />

              <CalendarModalSection>
                <TabletAndDesktop>
                  <HorizontalLine slim dashed />
                </TabletAndDesktop>

                <H2>{i18n.calendar.reservationModal.dateRange}</H2>

                <HolidayPeriodInfoBox holidayPeriods={holidayPeriods} />

                <Label>{i18n.calendar.reservationModal.selectRecurrence}</Label>
                <Gap size="xxs" />
                <SelectF bind={repetition} data-qa="repetition" />
                <Gap size="s" />

                <ExpandingInfo
                  width="auto"
                  info={
                    dayProperties.maxDate !== undefined ? (
                      <>
                        {i18n.calendar.reservationModal.dateRangeInfo(
                          dayProperties.maxDate
                        )}
                        {anyShiftCare && i18n.calendar.shiftCareInfo()}
                      </>
                    ) : (
                      i18n.calendar.reservationModal.noReservableDays
                    )
                  }
                  inlineChildren
                >
                  <Label>{i18n.calendar.reservationModal.dateRangeLabel}</Label>
                </ExpandingInfo>
                <DateRangePickerF
                  bind={dateRange}
                  locale={lang}
                  hideErrorsBeforeTouched={!showAllErrors}
                  onFocus={(ev) => {
                    scrollIntoViewSoftKeyboard(ev.target, 'start')
                  }}
                />

                {overlappingClosedHolidayPeriods.length > 0 && (
                  <InfoBox
                    title={i18n.calendar.closedHolidayPeriodAbsence.title(
                      overlappingClosedHolidayPeriods
                    )}
                    message={
                      i18n.calendar.closedHolidayPeriodAbsence.infoMessage
                    }
                  />
                )}

                <Gap size="m" />

                {selectedRange ? (
                  <RepetitionTimeInputGrid
                    bind={times}
                    showAllErrors={showAllErrors}
                  />
                ) : (
                  <MissingDateRange>
                    {i18n.calendar.reservationModal.missingDateRange}
                  </MissingDateRange>
                )}
              </CalendarModalSection>
            </div>
            <Gap size="m" />
            {saveError !== undefined && (
              <AlertBox
                title={i18n.calendar.reservationModal.saveErrors.failure}
                message={saveError}
                wide
                noMargin
              />
            )}
            <CalendarModalButtons>
              <LegacyButton
                onClick={onClose}
                data-qa="modal-cancelBtn"
                text={i18n.common.cancel}
              />
              <MutateButton
                primary
                text={i18n.common.confirm}
                disabled={
                  form.state.selectedChildren.length === 0 || !form.isValid()
                }
                mutation={postReservationsMutation}
                onClick={() => {
                  if (!form.isValid()) {
                    setShowAllErrors(true)
                    return cancelMutation
                  } else {
                    const request = form.value().toRequest(dayProperties)
                    return request.length > 0
                      ? { body: request }
                      : cancelMutation
                  }
                }}
                onSuccess={onSuccess}
                onFailure={(reason) => showSaveError(reason)}
                data-qa="modal-okBtn"
              />
            </CalendarModalButtons>
          </BottomFooterContainer>
        </CalendarModalBackground>
        <CalendarModalCloseButton
          onClick={onClose}
          aria-label={i18n.common.closeModal}
          icon={faTimes}
        />
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

const HolidayPeriodInfoBox = React.memo(function HolidayPeriodInfoBox({
  holidayPeriods
}: {
  holidayPeriods: HolidayPeriod[]
}) {
  const i18n = useTranslation()

  const today = LocalDate.todayInHelsinkiTz()
  const openHolidayPeriod: HolidayPeriod | undefined = useMemo(
    () =>
      holidayPeriods.find(
        (h) =>
          h.reservationsOpenOn.isEqualOrBefore(today) &&
          today.isEqualOrBefore(h.reservationDeadline)
      ),
    [holidayPeriods, today]
  )

  return openHolidayPeriod !== undefined ? (
    <InfoBox
      wide
      message={i18n.calendar.reservationModal.holidayPeriod(
        openHolidayPeriod.period
      )}
    />
  ) : null
})

const MissingDateRange = styled(Light)`
  grid-column-start: 1;
  grid-column-end: 4;
`
