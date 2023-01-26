// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { ErrorsOf, getErrorCount } from 'lib-common/form-validation'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import {
  AbsenceRequest,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { useMutationResult } from 'lib-common/query'
import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { ChoiceChip, SelectionChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import {
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
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
import { postAbsencesMutation } from './queries'

interface Props {
  close: () => void
  availableChildren: ReservationChild[]
  initialDate: LocalDate | undefined
  firstPlannedAbsenceDate: LocalDate | null
}

function initialForm(
  initialDate: LocalDate | undefined,
  availableChildren: ReservationChild[]
): Form {
  const selectedChildren = availableChildren.map((child) => child.id)
  if (initialDate) {
    return {
      startDate: initialDate,
      endDate: initialDate,
      selectedChildren
    }
  }
  return {
    startDate: LocalDate.todayInSystemTz(),
    endDate: null,
    selectedChildren
  }
}

export default React.memo(function AbsenceModal({
  close,
  availableChildren,
  initialDate,
  firstPlannedAbsenceDate
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const { mutateAsync: postAbsences } = useMutationResult(postAbsencesMutation)
  const [form, setForm] = useState<Form>(() =>
    initialForm(initialDate, availableChildren)
  )

  const updateForm = useCallback(
    (partialForm: Partial<Form>) =>
      setForm((previous) => {
        const form = { ...previous, ...partialForm }
        const contractDayAbsenceTypeSettings =
          getContractDayAbsenceTypeSettings(
            form,
            firstPlannedAbsenceDate,
            availableChildren
          )
        return {
          ...form,
          ...(form.absenceType === 'PLANNED_ABSENCE' &&
          featureFlags.citizenContractDayAbsence &&
          (!contractDayAbsenceTypeSettings.visible ||
            !contractDayAbsenceTypeSettings.enabled)
            ? { absenceType: undefined }
            : undefined)
        }
      }),
    [setForm, firstPlannedAbsenceDate, availableChildren]
  )

  const [showAllErrors, setShowAllErrors] = useState(false)
  const [request, errors] = validateForm(form)

  const showShiftCareAbsenceType = useMemo(
    () =>
      featureFlags.citizenShiftCareAbsence
        ? availableChildren.some(({ inShiftCareUnit }) => inShiftCareUnit)
        : false,
    [availableChildren]
  )
  const contractDayAbsenceTypeSettings = useMemo(
    () =>
      getContractDayAbsenceTypeSettings(
        form,
        firstPlannedAbsenceDate,
        availableChildren
      ),
    [form, firstPlannedAbsenceDate, availableChildren]
  )

  return (
    <ModalAccessibilityWrapper>
      <PlainModal mobileFullScreen margin="auto">
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
                  {availableChildren.map((child) => (
                    <SelectionChip
                      key={child.id}
                      text={formatFirstName(child)}
                      selected={form.selectedChildren.includes(child.id)}
                      onChange={(checked) =>
                        updateForm({
                          selectedChildren: checked
                            ? [...form.selectedChildren, child.id]
                            : form.selectedChildren.filter(
                                (id) => id !== child.id
                              )
                        })
                      }
                      data-qa={`child-${child.id}`}
                    />
                  ))}
                </FixedSpaceFlexWrap>
              </CalendarModalSection>
              <Gap size="zero" sizeOnMobile="s" />
              <LineContainer>
                <HorizontalLine dashed hiddenOnMobile slim />
              </LineContainer>
              <CalendarModalSection>
                <H2>{i18n.calendar.absenceModal.dateRange}</H2>
                <DateRangePicker
                  start={form.startDate}
                  end={form.endDate}
                  onChange={(startDate, endDate) =>
                    updateForm({ startDate, endDate })
                  }
                  locale={lang}
                  hideErrorsBeforeTouched={!showAllErrors}
                  onFocus={(ev) => {
                    scrollIntoViewSoftKeyboard(ev.target, 'start')
                  }}
                  startInfo={
                    errors &&
                    errorToInputInfo(errors.startDate, i18n.validationErrors)
                  }
                  endInfo={
                    errors &&
                    errorToInputInfo(errors.endDate, i18n.validationErrors)
                  }
                  minDate={LocalDate.todayInSystemTz()}
                />
                <Gap size="s" />
                <P noMargin>{i18n.calendar.absenceModal.selectChildrenInfo}</P>
              </CalendarModalSection>
              <Gap size="zero" sizeOnMobile="s" />
              <LineContainer>
                <HorizontalLine dashed hiddenOnMobile slim />
              </LineContainer>
              <CalendarModalSection>
                <H2>{i18n.calendar.absenceModal.absenceType}</H2>
                {contractDayAbsenceTypeSettings.visible &&
                  !contractDayAbsenceTypeSettings.enabled && (
                    <AlertBox
                      message={
                        i18n.calendar.absenceModal.contractDayAbsenceTypeWarning
                      }
                    />
                  )}
                <FixedSpaceFlexWrap verticalSpacing="xs">
                  <ChoiceChip
                    text={i18n.calendar.absenceModal.absenceTypes.SICKLEAVE}
                    selected={form.absenceType === 'SICKLEAVE'}
                    onChange={(selected) =>
                      updateForm({
                        absenceType: selected ? 'SICKLEAVE' : undefined
                      })
                    }
                    data-qa="absence-SICKLEAVE"
                  />
                  <ChoiceChip
                    text={i18n.calendar.absenceModal.absenceTypes.OTHER_ABSENCE}
                    selected={form.absenceType === 'OTHER_ABSENCE'}
                    onChange={(selected) =>
                      updateForm({
                        absenceType: selected ? 'OTHER_ABSENCE' : undefined
                      })
                    }
                    data-qa="absence-OTHER_ABSENCE"
                  />
                  {(showShiftCareAbsenceType ||
                    contractDayAbsenceTypeSettings.visible) && (
                    <>
                      <ChoiceChip
                        text={
                          i18n.calendar.absenceModal.absenceTypes
                            .PLANNED_ABSENCE
                        }
                        selected={form.absenceType === 'PLANNED_ABSENCE'}
                        onChange={(selected) =>
                          updateForm({
                            absenceType: selected
                              ? 'PLANNED_ABSENCE'
                              : undefined
                          })
                        }
                        disabled={
                          contractDayAbsenceTypeSettings.visible &&
                          !contractDayAbsenceTypeSettings.enabled
                        }
                        data-qa="absence-PLANNED_ABSENCE"
                      />
                    </>
                  )}
                </FixedSpaceFlexWrap>
                {showAllErrors && !form.absenceType ? (
                  <Warning data-qa="modal-absence-type-required-error">
                    {i18n.validationErrors.requiredSelection}
                  </Warning>
                ) : null}
              </CalendarModalSection>
            </div>
            <CalendarModalButtons>
              <Button
                onClick={close}
                data-qa="modal-cancelBtn"
                text={i18n.common.cancel}
              />
              <AsyncButton
                primary
                text={i18n.common.confirm}
                disabled={form.selectedChildren.length === 0}
                onClick={() => {
                  if (!request) {
                    setShowAllErrors(true)
                    return
                  }

                  return postAbsences(request)
                }}
                onSuccess={close}
                data-qa="modal-okBtn"
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

interface Form {
  startDate: LocalDate | null
  endDate: LocalDate | null
  selectedChildren: string[]
  absenceType?: AbsenceType
}

const validateForm = (
  form: Form
): [AbsenceRequest] | [undefined, ErrorsOf<Form>] => {
  const errors: ErrorsOf<Form> = {
    startDate: !form.startDate ? 'required' : undefined,
    endDate: !form.endDate ? 'required' : undefined,
    absenceType: form.absenceType === undefined ? 'required' : undefined
  }

  if (getErrorCount(errors) > 0) {
    return [undefined, errors]
  }

  return [
    {
      childIds: form.selectedChildren,
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      dateRange: new FiniteDateRange(form.startDate!, form.endDate!),
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      absenceType: form.absenceType!
    }
  ]
}

const getContractDayAbsenceTypeSettings = (
  form: Form,
  firstPlannedAbsenceDate: LocalDate | null,
  availableChildren: ReservationChild[]
) => {
  if (
    featureFlags.citizenContractDayAbsence &&
    form.startDate !== null &&
    firstPlannedAbsenceDate !== null &&
    form.startDate >= firstPlannedAbsenceDate
  ) {
    const selectedChildren = availableChildren.filter(({ id }) =>
      form.selectedChildren.includes(id)
    )
    return {
      visible: selectedChildren.some(({ hasContractDays }) => hasContractDays),
      enabled:
        selectedChildren.length > 0 &&
        selectedChildren.every(({ hasContractDays }) => hasContractDays)
    }
  }
  return { visible: false, enabled: false }
}

const Warning = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

export const LineContainer = styled.div`
  padding: 0 ${defaultMargins.L};
`
