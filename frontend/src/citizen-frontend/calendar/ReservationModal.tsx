// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatPreferredName } from 'lib-common/names'
import {
  Repetition,
  ReservationFormDataForValidation,
  validateForm
} from 'lib-common/reservations'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { SelectionChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Select from 'lib-components/atoms/dropdowns/Select'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import {
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, Label, Light } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'

import { BottomFooterContainer } from './BottomFooterContainer'
import {
  CalendarModalBackground,
  CalendarModalButtons,
  CalendarModalCloseButton,
  CalendarModalSection
} from './CalendarModal'
import { postReservations } from './api'
import RepetitionTimeInputGrid from './reservation-modal/RepetitionTimeInputGrid'
import { getEarliestReservableDate, getLatestReservableDate } from './utils'

interface Props {
  onClose: () => void
  onSuccess: (containsNonReservableDays: boolean) => void
  availableChildren: ReservationChild[]
  reservableDays: Record<string, FiniteDateRange[]>
  initialStart: LocalDate | null
  initialEnd: LocalDate | null
  existingReservations: DailyReservationData[]
}

export default React.memo(function ReservationModal({
  onClose,
  onSuccess,
  availableChildren,
  reservableDays,
  initialStart,
  initialEnd,
  existingReservations
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [formData, setFormData] = useState<ReservationFormDataForValidation>({
    selectedChildren: availableChildren.map((child) => child.id),
    startDate: initialStart,
    endDate: initialEnd,
    repetition: 'DAILY',
    dailyTimes: [
      {
        startTime: '',
        endTime: ''
      }
    ],
    weeklyTimes: [0, 1, 2, 3, 4, 5, 6].map(() => [
      {
        startTime: '',
        endTime: ''
      }
    ]),
    irregularTimes: {}
  })

  const updateForm = useCallback(
    (updated: Partial<ReservationFormDataForValidation>) => {
      setFormData((prev) => ({
        ...prev,
        ...updated
      }))
    },
    []
  )

  const [showAllErrors, setShowAllErrors] = useState(false)
  const validationResult = useMemo(
    () => validateForm(reservableDays, formData),
    [reservableDays, formData]
  )

  const isInvalidDate = useCallback(
    (date: LocalDate) =>
      availableChildren.some((child) =>
        reservableDays[child.id].some((r) => r.includes(date))
      )
        ? null
        : i18n.validationErrors.unselectableDate,
    [availableChildren, reservableDays, i18n]
  )

  const { minDate, maxDate } = useMemo(
    () => ({
      minDate: getEarliestReservableDate(availableChildren, reservableDays),
      maxDate: getLatestReservableDate(availableChildren, reservableDays)
    }),
    [availableChildren, reservableDays]
  )

  const selectedRange = useMemo(() => {
    if (!formData.startDate || !formData.endDate) {
      return
    }

    return new FiniteDateRange(formData.startDate, formData.endDate)
  }, [formData.startDate, formData.endDate])

  const childrenInShiftCare = useMemo(
    () => availableChildren.some(({ inShiftCareUnit }) => inShiftCareUnit),
    [availableChildren]
  )

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
                      text={formatPreferredName(child)}
                      selected={formData.selectedChildren.includes(child.id)}
                      onChange={(selected) => {
                        if (selected) {
                          updateForm({
                            selectedChildren: [
                              ...formData.selectedChildren,
                              child.id
                            ]
                          })
                        } else {
                          updateForm({
                            selectedChildren: formData.selectedChildren.filter(
                              (id) => id !== child.id
                            )
                          })
                        }
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
                <Select<Repetition>
                  items={['DAILY', 'WEEKLY', 'IRREGULAR']}
                  selectedItem={formData.repetition}
                  onChange={(value) => {
                    if (value) updateForm({ repetition: value })
                  }}
                  getItemLabel={(item) =>
                    i18n.calendar.reservationModal.repetitions[item]
                  }
                  data-qa="repetition"
                />
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
                <DateRangePicker
                  start={formData.startDate}
                  end={formData.endDate}
                  onChange={(startDate, endDate) =>
                    updateForm({ startDate, endDate })
                  }
                  locale={lang}
                  isInvalidDate={isInvalidDate}
                  minDate={minDate}
                  maxDate={maxDate}
                  hideErrorsBeforeTouched={!showAllErrors}
                  onFocus={(ev) => {
                    scrollIntoViewSoftKeyboard(ev.target, 'start')
                  }}
                  startInfo={errorToInputInfo(
                    validationResult.errors?.startDate,
                    i18n.validationErrors
                  )}
                  endInfo={errorToInputInfo(
                    validationResult.errors?.endDate,
                    i18n.validationErrors
                  )}
                />

                <Gap size="m" />

                {selectedRange ? (
                  <RepetitionTimeInputGrid
                    formData={formData}
                    childrenInShiftCare={childrenInShiftCare}
                    includedDays={includedDays}
                    updateForm={updateForm}
                    showAllErrors={showAllErrors}
                    existingReservations={existingReservations}
                    validationResult={validationResult}
                    selectedRange={selectedRange}
                    repetition={formData.repetition}
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
                disabled={formData.selectedChildren.length === 0}
                onClick={() => {
                  if (validationResult.errors) {
                    setShowAllErrors(true)
                    return
                  } else {
                    return postReservations(validationResult.requestPayload)
                  }
                }}
                onSuccess={() => {
                  onSuccess(
                    !validationResult.errors
                      ? validationResult.containsNonReservableDays
                      : false
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
