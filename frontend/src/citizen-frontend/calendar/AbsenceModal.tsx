// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import { Failure } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { localDateRange } from 'lib-common/form/fields'
import { array, mapped, object, required, value } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { AbsenceType } from 'lib-common/generated/api-types/absence'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import {
  AbsenceRequest,
  ReservationChild,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { SelectionChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import { TabletAndDesktop } from 'lib-components/layout/responsive-layout'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import {
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
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
import { postAbsencesMutation } from './queries'

const absenceForm = mapped(
  object({
    selectedChildren: array(value<UUID>()),
    range: required(localDateRange()),
    absenceType: required(value<AbsenceType | undefined>())
  }),
  (output): AbsenceRequest => ({
    childIds: output.selectedChildren,
    dateRange: output.range,
    absenceType: output.absenceType
  })
)

function initialFormState(
  initialDate: LocalDate | undefined,
  availableChildren: ReservationChild[],
  maxDate: LocalDate
): StateOf<typeof absenceForm> {
  const selectedChildren =
    availableChildren.length == 1 ? [availableChildren[0].id] : []
  const range =
    initialDate !== undefined
      ? { startDate: initialDate, endDate: initialDate }
      : { startDate: LocalDate.todayInSystemTz(), endDate: null }
  return {
    range: localDateRange.fromDates(range.startDate, range.endDate, {
      minDate: LocalDate.todayInSystemTz(),
      maxDate
    }),
    selectedChildren,
    absenceType: undefined
  }
}

interface Props {
  close: () => void
  onReturn: () => void
  reservationsResponse: ReservationsResponse
  initialDate: LocalDate | undefined
  holidayPeriods: HolidayPeriod[]
}

export default React.memo(function AbsenceModal({
  close,
  onReturn,
  reservationsResponse,
  initialDate,
  holidayPeriods
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const showShiftCareAbsenceType = useMemo(
    () =>
      featureFlags.citizenShiftCareAbsence
        ? reservationsResponse.days.some((day) =>
            day.children.some(({ shiftCare }) => shiftCare)
          )
        : false,
    [reservationsResponse.days]
  )

  const form = useForm(
    absenceForm,
    () =>
      initialFormState(
        initialDate,
        reservationsResponse.children,
        reservationsResponse.reservableRange.end
      ),
    i18n.validationErrors
  )
  const { selectedChildren, range, absenceType } = useFormFields(form)

  const [showAllErrors, useShowAllErrors] = useBoolean(false)

  const duplicateChildInfo = getDuplicateChildInfo(
    reservationsResponse.children,
    i18n
  )

  const absencesWarning =
    range.isValid() &&
    range
      .value()
      .complement(
        new DateRange(reservationsResponse.reservableRange.start, null)
      )
      .reduce((sum, r) => sum + r.durationInDays(), 0) > 1

  const closedHolidayPeriods = range.isValid()
    ? holidayPeriods
        .filter(
          (hp) =>
            hp.period.overlaps(range.value()) &&
            hp.reservationDeadline.isBefore(LocalDate.todayInHelsinkiTz())
        )
        .map((hp) => hp.period)
    : []

  const [attendanceAlreadyExistsError, setAttendanceAlreadyExistsError] =
    useState(false)

  return (
    <ModalAccessibilityWrapper>
      <PlainModal mobileFullScreen margin="auto" onEscapeKey={close}>
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
                  {i18n.calendar.absenceModal.title}
                </ModalHeader>
              </CalendarModalSection>
              <Gap size="s" />
              <CalendarModalSection>
                <H2>{i18n.calendar.absenceModal.selectedChildren}</H2>
                <Gap size="xs" />
                <FixedSpaceFlexWrap>
                  {reservationsResponse.children
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
                          onChange={(selected) =>
                            selectedChildren.update((prev) =>
                              selected
                                ? [...prev, child.id]
                                : prev.filter((id) => id !== child.id)
                            )
                          }
                          data-qa={`child-${child.id}`}
                        />
                      </div>
                    ))}
                </FixedSpaceFlexWrap>
              </CalendarModalSection>
              <Gap size="zero" sizeOnMobile="s" />
              <LineContainer>
                <TabletAndDesktop>
                  <HorizontalLine dashed slim />
                </TabletAndDesktop>
              </LineContainer>
              <CalendarModalSection>
                <H2>{i18n.calendar.absenceModal.dateRange}</H2>
                <DateRangePickerF
                  bind={range}
                  locale={lang}
                  hideErrorsBeforeTouched={!showAllErrors}
                  onFocus={(ev) => {
                    scrollIntoViewSoftKeyboard(ev.target, 'start')
                  }}
                />
                <Gap size="s" />
                <P noMargin>{i18n.calendar.absenceModal.selectChildrenInfo}</P>
                {absencesWarning ? (
                  <AlertBox
                    title={
                      i18n.calendar.absenceModal.lockedAbsencesWarningTitle
                    }
                    message={
                      i18n.calendar.absenceModal.lockedAbsencesWarningText
                    }
                  />
                ) : closedHolidayPeriods.length > 0 ? (
                  <AlertBox
                    title={i18n.calendar.closedHolidayPeriodAbsence.title(
                      closedHolidayPeriods
                    )}
                    message={
                      i18n.calendar.closedHolidayPeriodAbsence.warningMessage
                    }
                  />
                ) : null}
                {attendanceAlreadyExistsError && (
                  <AlertBox
                    title={
                      i18n.calendar.absenceModal
                        .attendanceAlreadyExistsErrorTitle
                    }
                    message={
                      i18n.calendar.absenceModal
                        .attendanceAlreadyExistsErrorDescription
                    }
                  />
                )}
              </CalendarModalSection>
              <Gap size="zero" sizeOnMobile="s" />
              <LineContainer>
                <TabletAndDesktop>
                  <HorizontalLine dashed slim />
                </TabletAndDesktop>
              </LineContainer>
              <CalendarModalSection>
                <H2 id="absence-type-heading">
                  {i18n.calendar.absenceModal.absenceType}
                </H2>
                <FixedSpaceFlexWrap verticalSpacing="xs">
                  <SelectionChip
                    text={i18n.calendar.absenceModal.absenceTypes.SICKLEAVE}
                    selected={absenceType.state === 'SICKLEAVE'}
                    onChange={(selected) =>
                      absenceType.set(selected ? 'SICKLEAVE' : undefined)
                    }
                    data-qa="absence-SICKLEAVE"
                    hideIcon
                  />
                  <SelectionChip
                    text={i18n.calendar.absenceModal.absenceTypes.OTHER_ABSENCE}
                    selected={absenceType.state === 'OTHER_ABSENCE'}
                    onChange={(selected) =>
                      absenceType.set(selected ? 'OTHER_ABSENCE' : undefined)
                    }
                    data-qa="absence-OTHER_ABSENCE"
                    hideIcon
                  />
                  {showShiftCareAbsenceType ? (
                    <>
                      <SelectionChip
                        text={
                          i18n.calendar.absenceModal.absenceTypes
                            .PLANNED_ABSENCE
                        }
                        selected={absenceType.state === 'PLANNED_ABSENCE'}
                        onChange={(selected) =>
                          absenceType.set(
                            selected ? 'PLANNED_ABSENCE' : undefined
                          )
                        }
                        data-qa="absence-PLANNED_ABSENCE"
                        hideIcon
                      />
                    </>
                  ) : null}
                </FixedSpaceFlexWrap>
                {showAllErrors && !absenceType.isValid() ? (
                  <Warning
                    data-qa="modal-absence-type-required-error"
                    role="alert"
                    aria-labelledby="absence-type-heading"
                  >
                    {i18n.validationErrors.requiredSelection}
                  </Warning>
                ) : null}
              </CalendarModalSection>
            </div>
            <CalendarModalButtons>
              <LegacyButton
                onClick={onReturn}
                data-qa="modal-cancelBtn"
                text={i18n.common.cancel}
              />
              <MutateButton
                primary
                text={i18n.common.confirm}
                disabled={selectedChildren.state.length === 0}
                mutation={postAbsencesMutation}
                onClick={() => {
                  if (!form.isValid()) {
                    useShowAllErrors.on()
                    return cancelMutation
                  }
                  return { body: form.value() }
                }}
                onSuccess={close}
                data-qa="modal-okBtn"
                onFailure={(failure: Failure<unknown>) => {
                  setAttendanceAlreadyExistsError(
                    failure.errorCode === 'ATTENDANCE_ALREADY_EXISTS'
                  )
                }}
              />
            </CalendarModalButtons>
          </BottomFooterContainer>
        </CalendarModalBackground>
        <CalendarModalCloseButton
          onClick={close}
          aria-label={i18n.common.closeModal}
          icon={faTimes}
        />
      </PlainModal>
    </ModalAccessibilityWrapper>
  )
})

const Warning = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

export const LineContainer = styled.div`
  padding: 0 ${defaultMargins.L};
`
