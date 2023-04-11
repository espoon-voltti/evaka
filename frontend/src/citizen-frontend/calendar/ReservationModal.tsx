// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import { Failure } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { combine } from 'lib-common/form/types'
import {
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { useMutationResult } from 'lib-common/query'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { SelectionChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
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
  HolidayPeriodInfo,
  initialState,
  reservationForm,
  resetTimes
} from './reservation-modal/form'
import { getEarliestReservableDate, getLatestReservableDate } from './utils'

interface Props {
  onClose: () => void
  onSuccess: (containsNonReservableDays: boolean) => void
  availableChildren: ReservationChild[]
  reservableDays: Record<string, FiniteDateRange[]>
  initialStart: LocalDate | null
  initialEnd: LocalDate | null
  existingReservations: DailyReservationData[]
  upcomingHolidayPeriods: HolidayPeriodInfo[]
}

export default React.memo(function ReservationModal({
  onClose,
  onSuccess,
  availableChildren,
  reservableDays,
  initialStart,
  initialEnd,
  existingReservations,
  upcomingHolidayPeriods
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const childrenInShiftCare = useMemo(
    () =>
      availableChildren.some(
        ({ maxOperationalDays }) => maxOperationalDays.length == 7
      ),
    [availableChildren]
  )

  const form = useForm(
    reservationForm,
    () =>
      initialState(
        availableChildren,
        initialStart,
        initialEnd,
        childrenInShiftCare,
        existingReservations,
        upcomingHolidayPeriods,
        i18n
      ),
    i18n.validationErrors,
    {
      onUpdate: (prevState, nextState, form) => {
        const prev = combine(
          form.shape.repetition.validate(prevState.repetition),
          form.shape.dateRange.validate(prevState.dateRange),
          form.shape.selectedChildren.validate(prevState.selectedChildren)
        )
        const next = combine(
          form.shape.repetition.validate(nextState.repetition),
          form.shape.dateRange.validate(nextState.dateRange),
          form.shape.selectedChildren.validate(nextState.selectedChildren)
        )

        if (!next.isValid) return nextState
        const [repetition, selectedRange, selectedChildren] = next.value

        if (!prev.isValid) {
          return {
            ...nextState,
            times: resetTimes(
              childrenInShiftCare,
              existingReservations,
              repetition,
              selectedRange,
              selectedChildren,
              upcomingHolidayPeriods
            )
          }
        }

        const [prevRepetition, prevDateRange, prevSelectedChildren] = prev.value
        if (
          selectedRange !== undefined &&
          (prevRepetition !== repetition ||
            prevDateRange === undefined ||
            !prevDateRange.isEqual(selectedRange) ||
            !isEqual(prevSelectedChildren, selectedChildren))
        ) {
          return {
            ...nextState,
            times: resetTimes(
              childrenInShiftCare,
              existingReservations,
              repetition,
              selectedRange,
              selectedChildren,
              upcomingHolidayPeriods
            )
          }
        }
        return nextState
      }
    }
  )

  const { selectedChildren, repetition, dateRange, times } = useFormFields(form)

  const [showAllErrors, setShowAllErrors] = useState(false)

  const { minDate, maxDate } = useMemo(
    () => ({
      minDate: getEarliestReservableDate(availableChildren, reservableDays),
      maxDate: getLatestReservableDate(availableChildren, reservableDays)
    }),
    [availableChildren, reservableDays]
  )

  const selectedRange = dateRange.isValid() ? dateRange.value() : undefined

  const includedDays = useMemo(() => {
    if (!selectedRange) return []

    const combinedOperationDays = availableChildren.reduce<number[]>(
      (totalOperationDays, child) => [
        ...new Set([...totalOperationDays, ...child.maxOperationalDays])
      ],
      []
    )

    return [1, 2, 3, 4, 5, 6, 7].filter(
      (day) =>
        combinedOperationDays.includes(day) &&
        [...selectedRange.dates()].some(
          (date) => date.getIsoDayOfWeek() === day
        )
    )
  }, [availableChildren, selectedRange])

  const { mutateAsync: postReservations } = useMutationResult(
    postReservationsMutation
  )

  const [saveError, setSaveError] = useState<string | undefined>()
  const showSaveError = useCallback(
    (reason: Failure<void>) => {
      reason.errorCode === 'NON_RESERVABLE_DAYS' &&
        setSaveError(
          i18n.calendar.reservationModal.saveErrors.NON_RESERVABLE_DAYS
        )
    },
    [i18n, setSaveError]
  )

  const duplicateChildInfo = getDuplicateChildInfo(availableChildren, i18n)

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
                <Label>
                  {i18n.calendar.reservationModal.selectChildrenLabel}
                </Label>
                <Gap size="xs" />
                <FixedSpaceFlexWrap>
                  {availableChildren.map((child) => (
                    <SelectionChip
                      key={child.id}
                      text={`${formatFirstName(child)}${
                        duplicateChildInfo[child.id] !== undefined
                          ? ` ${duplicateChildInfo[child.id]}`
                          : ''
                      }`}
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
                  ))}
                </FixedSpaceFlexWrap>
              </CalendarModalSection>

              <Gap size="xxs" sizeOnMobile="s" />

              <CalendarModalSection>
                <HorizontalLine slim dashed hiddenOnMobile />

                <H2>{i18n.calendar.reservationModal.dateRange}</H2>
                <Label>{i18n.calendar.reservationModal.selectRecurrence}</Label>
                <Gap size="xxs" />
                <SelectF bind={repetition} data-qa="repetition" />
                <Gap size="s" />

                <ExpandingInfo
                  width="auto"
                  ariaLabel={i18n.common.openExpandingInfo}
                  info={
                    Object.keys(reservableDays).length > 0
                      ? i18n.calendar.reservationModal.dateRangeInfo(maxDate)
                      : i18n.calendar.reservationModal.noReservableDays
                  }
                  inlineChildren
                  closeLabel={i18n.common.close}
                >
                  <Label>{i18n.calendar.reservationModal.dateRangeLabel}</Label>
                </ExpandingInfo>
                <DateRangePickerF
                  bind={dateRange}
                  locale={lang}
                  isInvalidDate={() => null} //TODO: isInvalidDate
                  minDate={minDate}
                  maxDate={maxDate}
                  hideErrorsBeforeTouched={!showAllErrors}
                  onFocus={(ev) => {
                    scrollIntoViewSoftKeyboard(ev.target, 'start')
                  }}
                />

                <Gap size="m" />

                {selectedRange ? (
                  <RepetitionTimeInputGrid
                    bind={times}
                    childrenInShiftCare={childrenInShiftCare}
                    includedDays={includedDays}
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
              <Button
                onClick={onClose}
                data-qa="modal-cancelBtn"
                text={i18n.common.cancel}
              />
              <AsyncButton
                primary
                text={i18n.common.confirm}
                disabled={form.state.selectedChildren.length === 0}
                onClick={() => {
                  if (!form.isValid()) {
                    setShowAllErrors(true)
                    return
                  } else {
                    return postReservations(
                      form.value().toRequest(reservableDays)
                    )
                  }
                }}
                onSuccess={() => {
                  onSuccess(
                    form.value().containsNonReservableDays(reservableDays)
                  )
                }}
                onFailure={(reason) => {
                  showSaveError(reason)
                }}
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

const MissingDateRange = styled(Light)`
  grid-column-start: 1;
  grid-column-end: 4;
`
