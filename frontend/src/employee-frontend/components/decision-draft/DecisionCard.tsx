// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import type {
  DecisionDraft,
  DecisionType
} from 'lib-common/generated/api-types/decision'
import type {
  DecisionIndividualReasoningId,
  OfficialLanguage
} from 'lib-common/generated/api-types/shared'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { Chip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights, Title } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'

import IndividualReasoningPickerModal from './IndividualReasoningPickerModal'
import { decisionTypeToCollectionType } from './decisionTypeToCollectionType'
import { getIndividualReasoningsQuery } from './queries'

const Card = styled.div<{ $twoColumn: boolean }>`
  border: 1px solid ${colors.grayscale.g15};
  border-radius: 4px;
  padding: ${defaultMargins.m};
  display: grid;
  grid-template-columns: ${({ $twoColumn }) =>
    $twoColumn ? 'auto 1fr' : '1fr'};
  column-gap: ${defaultMargins.s};
  align-items: start;
`

const Body = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.s};
`

const TitleLabel = styled.label`
  cursor: pointer;
  user-select: none;
  padding-top: ${defaultMargins.xs};
`

const DecisionWrapper = styled.div`
  display: flex;
  flex-direction: column;
  margin-top: ${defaultMargins.s};
  gap: ${defaultMargins.L};
`

const DateRange = styled.div`
  font-family: 'Montserrat', sans-serif;
  color: ${colors.grayscale.g70};
`

const SectionLabel = styled.div`
  font-weight: ${fontWeights.semibold};
`

const GenericHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.m};
`

const GenericCard = styled.div<{ $notReady: boolean }>`
  background: ${({ $notReady }) =>
    $notReady ? colors.grayscale.g0 : `${colors.main.m2}10`};
  ${({ $notReady }) =>
    $notReady ? `border: 1px solid ${colors.grayscale.g15};` : ''}
  border-left: ${defaultMargins.xxs} solid
    ${({ $notReady }) => ($notReady ? colors.status.warning : colors.main.m2)};
  padding: ${defaultMargins.s};
  border-radius: ${defaultMargins.xxs};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xs};
`

const GenericTitle = styled.div`
  font-weight: ${fontWeights.semibold};
`

const NotReadyDescription = styled.div`
  color: ${colors.grayscale.g70};
  font-size: 14px;
  margin-bottom: ${defaultMargins.xs};
`

const IndividualHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.s};
`

const IndividualCard = styled.div`
  background: ${colors.accents.a4violet}10;
  border-left: 4px solid ${colors.accents.a4violet};
  padding: ${defaultMargins.s};
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
`

const IndividualTitle = styled.div`
  font-weight: ${fontWeights.semibold};
`

const Muted = styled.div`
  color: ${colors.grayscale.g70};
  font-style: italic;
`

interface Props {
  decision: DecisionDraft
  childName: string
  showPlannedCheckbox: boolean
  primaryDecisionType: DecisionType
  language: OfficialLanguage
  unitLanguageUnsupported: boolean
  onPlannedChange: (planned: boolean) => void
  onReasoningIdsChange: (
    ids: ReadonlySet<DecisionIndividualReasoningId>
  ) => void
}

/**
 * When the primary decision is PREPARATORY_EDUCATION, connected care decisions
 * (PRESCHOOL_DAYCARE or PRESCHOOL_CLUB) should be labeled as PREPARATORY_DAYCARE
 * to match the educational context, even though they share the same underlying type.
 */
const decisionTypeForLabel = (
  type: DecisionType,
  primaryDecisionType: DecisionType
) =>
  (type === 'PRESCHOOL_DAYCARE' || type === 'PRESCHOOL_CLUB') &&
  primaryDecisionType === 'PREPARATORY_EDUCATION'
    ? 'PREPARATORY_DAYCARE'
    : type

export default React.memo(function DecisionCard({
  decision,
  childName,
  showPlannedCheckbox,
  primaryDecisionType,
  language,
  unitLanguageUnsupported,
  onPlannedChange,
  onReasoningIdsChange
}: Props) {
  const { i18n } = useTranslation()
  const { featureConfig } = useContext(UserContext)
  const [pickerOpen, setPickerOpen] = useState(false)
  const checkboxId = useUniqueId('planned')
  const reasoningsExempt =
    featureConfig?.decisionsWithoutReasonings.includes(decision.type) ?? false
  const individualReasoningsResult = useQueryResult(
    reasoningsExempt
      ? constantQuery([])
      : getIndividualReasoningsQuery({
          collectionType: decisionTypeToCollectionType(decision.type),
          language
        })
  )

  const typeLabel =
    i18n.decisionDraft.types[
      decisionTypeForLabel(decision.type, primaryDecisionType)
    ]

  const selectedIds = new Set(decision.individualReasoningIds)
  const genericText = (s: { textFi: string; textSv: string }) =>
    language === 'SV' ? s.textSv : s.textFi

  return (
    <Card
      $twoColumn={showPlannedCheckbox}
      data-qa={`decision-card-${decision.type}`}
    >
      {showPlannedCheckbox && (
        <Checkbox
          id={checkboxId}
          label={typeLabel}
          hiddenLabel
          checked={decision.planned}
          onChange={onPlannedChange}
          data-qa={`planned-${decision.type}`}
        />
      )}
      <Body>
        {showPlannedCheckbox ? (
          <TitleLabel htmlFor={checkboxId}>
            <Title>{typeLabel}</Title>
          </TitleLabel>
        ) : (
          <Title>{typeLabel}</Title>
        )}
        <DateRange>
          {decision.startDate?.format() ?? ''}–
          {decision.endDate?.format() ?? ''}
        </DateRange>

        {unitLanguageUnsupported ? (
          <AlertBox
            data-qa="unit-language-unsupported-warning"
            title={i18n.decisionDraft.reasonings.unitLanguageUnsupported}
          />
        ) : reasoningsExempt ? null : (
          <DecisionWrapper>
            <div>
              <GenericHeader>
                <SectionLabel>
                  {i18n.decisionDraft.reasonings.generic}
                </SectionLabel>
              </GenericHeader>
              {decision.genericReasoning ? (
                <GenericCard
                  $notReady={!decision.genericReasoning.ready}
                  data-qa={`generic-card-${decision.genericReasoning.collectionType}`}
                >
                  {!decision.genericReasoning.ready && (
                    <>
                      <Chip
                        label={i18n.decisionReasonings.generic.statusNotReady}
                        size="small"
                        colorPalette="orange"
                        data-qa="not-ready-pill"
                      />
                      <NotReadyDescription>
                        {i18n.decisionReasonings.generic.notReadyWarning}
                      </NotReadyDescription>
                    </>
                  )}
                  <GenericTitle>
                    {decision.genericReasoning.endDate
                      ? i18n.decisionDraft.reasonings.genericRangeClosed(
                          decision.genericReasoning.validFrom.format(),
                          decision.genericReasoning.endDate.format()
                        )
                      : i18n.decisionDraft.reasonings.genericRangeOpen(
                          decision.genericReasoning.validFrom.format()
                        )}
                  </GenericTitle>
                  <span>{genericText(decision.genericReasoning)}</span>
                </GenericCard>
              ) : (
                <AlertBox
                  title={i18n.decisionDraft.reasonings.noGenericForSlot}
                />
              )}
            </div>

            <div>
              <IndividualHeader>
                <SectionLabel>
                  {i18n.decisionDraft.reasonings.individual}
                </SectionLabel>
                <Button
                  appearance="button"
                  icon={faFile}
                  onClick={() => setPickerOpen(true)}
                  text={i18n.decisionDraft.reasonings.pickerButton}
                  data-qa={`open-picker-${decision.type}`}
                />
              </IndividualHeader>
              {renderResult(individualReasoningsResult, (allReasonings) => {
                const selected = allReasonings.filter((r) =>
                  selectedIds.has(r.id)
                )
                return selected.length === 0 ? (
                  <Muted>{i18n.decisionDraft.reasonings.noIndividual}</Muted>
                ) : (
                  <FixedSpaceColumn $spacing="s">
                    {selected.map((ind) => (
                      <IndividualCard
                        key={ind.id}
                        data-qa={`individual-card-${ind.id}`}
                      >
                        <IndividualTitle>{ind.title}</IndividualTitle>
                        <span>{ind.text}</span>
                      </IndividualCard>
                    ))}
                  </FixedSpaceColumn>
                )
              })}
            </div>
          </DecisionWrapper>
        )}
      </Body>

      {pickerOpen && !unitLanguageUnsupported && !reasoningsExempt && (
        <IndividualReasoningPickerModal
          decisionType={decision.type}
          childName={childName}
          decisionTypeLabel={typeLabel}
          selectedIds={selectedIds}
          language={language}
          onChange={onReasoningIdsChange}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </Card>
  )
})
