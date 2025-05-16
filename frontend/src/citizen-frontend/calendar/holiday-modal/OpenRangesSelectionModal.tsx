// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  HolidayQuestionnaire,
  HolidayQuestionnaireAnswer,
  OpenRangesBody
} from 'lib-common/generated/api-types/holidayperiod'
import type { ReservationChild } from 'lib-common/generated/api-types/reservations'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { formatFirstName } from 'lib-common/names'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'

import ModalAccessibilityWrapper from '../../ModalAccessibilityWrapper'
import { useLang, useTranslation } from '../../localization'
import { getDuplicateChildInfo } from '../../utils/duplicated-child-utils'
import { answerOpenRangesQuestionnaireMutation } from '../queries'

import { RangeSelector } from './RangesSelector'

type FormState = OpenRangesBody['openRanges']

const initializeForm = (
  children: ReservationChild[],
  previousAnswers: HolidayQuestionnaireAnswer[]
): FormState =>
  children.reduce(
    (acc, child) => ({
      ...acc,
      [child.id]:
        previousAnswers.find((a) => a.childId === child.id)?.openRanges ?? []
    }),
    {}
  )

interface Props {
  close: () => void
  questionnaire: HolidayQuestionnaire.OpenRangesQuestionnaire
  availableChildren: ReservationChild[]
  eligibleChildren: Partial<Record<ChildId, FiniteDateRange[]>>
  previousAnswers: HolidayQuestionnaireAnswer[]
}

export default React.memo(function OpenRangesSelectionModal({
  close,
  questionnaire,
  availableChildren,
  eligibleChildren,
  previousAnswers
}: Props) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const [openRanges, setOpenRanges] = useState<FormState>(() =>
    initializeForm(availableChildren, previousAnswers)
  )

  const selectRanges = useCallback(
    (childId: string) => (ranges: FiniteDateRange[]) =>
      setOpenRanges((prev) => ({
        ...prev,
        [childId]: ranges
      })),
    [setOpenRanges]
  )

  const duplicateChildInfo = getDuplicateChildInfo(availableChildren, i18n)

  return (
    <ModalAccessibilityWrapper>
      <MutateFormModal
        mobileFullScreen
        width="wide"
        title={questionnaire.title[lang]}
        resolveMutation={answerOpenRangesQuestionnaireMutation}
        resolveAction={() => ({
          id: questionnaire.id,
          body: { openRanges }
        })}
        resolveLabel={i18n.common.confirm}
        onSuccess={close}
        rejectAction={close}
        rejectLabel={i18n.common.cancel}
        data-qa="open-ranges-selection-modal"
      >
        <FixedSpaceColumn>
          <HolidaySection>
            <div>{questionnaire.description[lang]}</div>
            <div>{questionnaire.period.format()}</div>
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
              {eligibleChildren[child.id] !== undefined ? (
                <RangeSelector
                  period={questionnaire.period}
                  value={openRanges[child.id] ?? []}
                  onSelectRanges={selectRanges(child.id)}
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
