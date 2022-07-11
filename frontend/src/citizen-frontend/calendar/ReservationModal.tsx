// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import maxBy from 'lodash/maxBy'
import minBy from 'lodash/minBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

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
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { PlainModal } from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, Label, Light } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { errorToInputInfo } from '../input-info-helper'
import { useLang, useTranslation } from '../localization'

import { BottomFooterContainer } from './BottomFooterContainer'
import { postReservations } from './api'
import RepetitionTimeInputGrid from './reservation-modal/RepetitionTimeInputGrid'

interface Props {
  onClose: () => void
  onReload: () => void
  availableChildren: ReservationChild[]
  reservableDays: FiniteDateRange[]
  firstReservableDate: LocalDate
  existingReservations: DailyReservationData[]
}

export default React.memo(function ReservationModal({
  onClose,
  onReload,
  availableChildren,
  reservableDays,
  firstReservableDate,
  existingReservations
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [formData, setFormData] = useState<ReservationFormDataForValidation>({
    selectedChildren: availableChildren.map((child) => child.id),
    startDate: firstReservableDate,
    endDate: null,
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
      reservableDays.some((r) => r.includes(date))
        ? null
        : i18n.validationErrors.unselectableDate,
    [reservableDays, i18n]
  )

  const { minDate, maxDate } = useMemo(
    () => ({
      minDate: minBy(reservableDays, (range) => range.start)?.start,
      maxDate: maxBy(reservableDays, (range) => range.end)?.end
    }),
    [reservableDays]
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

  return (
    <ModalAccessibilityWrapper>
      <PlainModal mobileFullScreen margin="auto">
        <ReservationModalBackground>
          <BottomFooterContainer>
            <div>
              <ModalSection>
                <Gap size="L" sizeOnMobile="zero" />
                <H1 data-qa="title" noMargin>
                  {i18n.calendar.reservationModal.title}
                </H1>
              </ModalSection>

              <Gap size="zero" sizeOnMobile="s" />

              <ModalSection>
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
              </ModalSection>

              <Gap size="xxs" sizeOnMobile="s" />

              <ModalSection>
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
                    reservableDays.length > 0
                      ? i18n.calendar.reservationModal.dateRangeInfo(
                          reservableDays[0].end
                        )
                      : i18n.calendar.reservationModal.noReservableDays
                  }
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
                  errorTexts={i18n.validationErrors}
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
              </ModalSection>
            </div>
            <ReservationModalButtons>
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
                  onReload()
                  onClose()
                }}
                data-qa="modal-okBtn"
              />
            </ReservationModalButtons>
          </BottomFooterContainer>
        </ReservationModalBackground>
        <ReservationModalCloseButton
          onClick={onClose}
          altText={i18n.common.closeModal}
          icon={faTimes}
        />
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

const ReservationModalCloseButton = styled(IconButton)`
  position: absolute;
  top: ${defaultMargins.s};
  right: ${defaultMargins.s};
  color: ${(p) => p.theme.colors.main.m2};
`

const ReservationModalButtons = styled.div`
  display: flex;
  justify-content: space-between;
  gap: ${defaultMargins.s};
  padding: ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
    display: grid;
    grid-template-columns: 1fr 1fr;
    background-color: ${(p) => p.theme.colors.main.m4};
  }
`

const ModalSection = styled.section`
  padding: 0 ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.m} ${defaultMargins.s};
    background-color: ${(p) => p.theme.colors.grayscale.g0};

    h2 {
      margin-top: 0;
    }
  }
`

const ReservationModalBackground = styled.div`
  height: 100%;

  @media (max-width: ${tabletMin}) {
    background-color: ${(p) => p.theme.colors.main.m4};
  }
`

const MissingDateRange = styled(Light)`
  grid-column-start: 1;
  grid-column-end: 4;
`
