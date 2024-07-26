// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { useLang, useTranslation } from 'citizen-frontend/localization'
import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  FixedPeriodQuestionnaire,
  FixedPeriodsBody,
  HolidayQuestionnaireAnswer
} from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatFirstName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { answerFixedPeriodQuestionnaireMutation } from '../queries'

import { PeriodSelector } from './PeriodSelector'

type FormState = FixedPeriodsBody['fixedPeriods']

const initializeForm = (
  children: ReservationChild[],
  previousAnswers: HolidayQuestionnaireAnswer[]
): FormState =>
  children.reduce(
    (acc, child) => ({
      ...acc,
      [child.id]:
        previousAnswers.find((a) => a.childId === child.id)?.fixedPeriod ?? null
    }),
    {}
  )

interface Props {
  close: () => void
  questionnaire: FixedPeriodQuestionnaire
  availableChildren: ReservationChild[]
  eligibleChildren: UUID[]
  previousAnswers: HolidayQuestionnaireAnswer[]
}

export default React.memo(function FixedPeriodSelectionModal({
  close,
  questionnaire,
  availableChildren,
  eligibleChildren,
  previousAnswers
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [fixedPeriods, setFixedPeriods] = useState<FormState>(() =>
    initializeForm(availableChildren, previousAnswers)
  )

  const selectPeriod = useCallback(
    (childId: string) => (period: FiniteDateRange | null) =>
      setFixedPeriods((prev) => ({ ...prev, [childId]: period })),
    [setFixedPeriods]
  )

  const duplicateChildInfo = getDuplicateChildInfo(availableChildren, i18n)

  return (
    <ModalAccessibilityWrapper>
      <MutateFormModal
        mobileFullScreen
        title={questionnaire.title[lang]}
        resolveMutation={answerFixedPeriodQuestionnaireMutation}
        resolveAction={() => ({
          id: questionnaire.id,
          body: { fixedPeriods }
        })}
        onSuccess={close}
        resolveLabel={i18n.common.confirm}
        rejectAction={close}
        rejectLabel={i18n.common.cancel}
        data-qa="fixed-period-selection-modal"
      >
        <FixedSpaceColumn>
          <HolidaySection>
            <div>{questionnaire.description[lang]}</div>
            <ExternalLink
              text={i18n.calendar.holidayModal.additionalInformation}
              href={questionnaire.descriptionLink[lang]}
              newTab
            />
          </HolidaySection>
          {availableChildren.map((child) => (
            <HolidaySection
              key={child.id}
              data-qa={`holiday-section-${child.id}`}
            >
              <H2 translate="no">
                {formatFirstName(child)}
                {duplicateChildInfo[child.id] !== undefined
                  ? ` ${duplicateChildInfo[child.id]}`
                  : ''}
              </H2>
              {eligibleChildren.includes(child.id) ? (
                <PeriodSelector
                  label={questionnaire.periodOptionLabel[lang]}
                  options={questionnaire.periodOptions}
                  value={fixedPeriods[child.id]}
                  onSelectPeriod={selectPeriod(child.id)}
                />
              ) : questionnaire.conditions.continuousPlacement ? (
                <div data-qa="not-eligible">
                  {i18n.calendar.holidayModal.notEligible(
                    questionnaire.conditions.continuousPlacement
                  )}
                </div>
              ) : null}
            </HolidaySection>
          ))}
        </FixedSpaceColumn>
      </MutateFormModal>
    </ModalAccessibilityWrapper>
  )
})

const HolidaySection = styled.div`
  background: white;
`
