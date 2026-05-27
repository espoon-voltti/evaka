// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import type {
  DecisionDraft,
  DecisionType,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import { Chip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { fontWeights, Title } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import IndividualReasoningPickerModal from './IndividualReasoningPickerModal'
import { draftReasoningPreviewQuery } from './queries'

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
  decisions: DecisionDraft[]
  childName: string
  units: DecisionUnit[]
  perDecisionUnitSelector: boolean
  onPlannedChange: (planned: boolean) => void
  onUnitChange: (unitId: DaycareId) => void
}

const decisionTypeForLabel = (
  type: DecisionType,
  decisions: DecisionDraft[]
) =>
  (type === 'PRESCHOOL_DAYCARE' || type === 'PRESCHOOL_CLUB') &&
  decisions.some((d) => d.type === 'PREPARATORY_EDUCATION')
    ? 'PREPARATORY_DAYCARE'
    : type

export default React.memo(function DecisionCard({
  decision,
  decisions,
  childName,
  units,
  perDecisionUnitSelector,
  onPlannedChange,
  onUnitChange
}: Props) {
  const { i18n, lang } = useTranslation()
  const [pickerOpen, setPickerOpen] = useState(false)
  const checkboxId = useUniqueId('planned')
  const previewResult = useQueryResult(
    draftReasoningPreviewQuery({ id: decision.id })
  )

  const typeLabel =
    i18n.decisionDraft.types[decisionTypeForLabel(decision.type, decisions)]
  const showPlannedCheckbox = decisions.length > 1
  const selectedUnit = units.find((u) => u.id === decision.unitId) ?? null

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

        {perDecisionUnitSelector && (
          <Combobox
            items={units}
            selectedItem={selectedUnit}
            onChange={(u) => u && onUnitChange(u.id)}
            getItemLabel={(u) => u?.name ?? ''}
            getItemDataQa={(u) => u?.id ?? ''}
            data-qa={`unit-${decision.type}`}
          />
        )}

        {renderResult(previewResult, (preview) => {
          const text = (s: { textFi: string; textSv: string | null }) =>
            lang === 'sv' ? (s.textSv ?? s.textFi) : s.textFi
          const title = (s: { titleFi: string; titleSv: string | null }) =>
            lang === 'sv' ? (s.titleSv ?? s.titleFi) : s.titleFi
          const linkedIds = new Set(
            preview.individualReasonings.map((r) => r.id)
          )
          const generic = preview.genericReasoning
          return (
            <DecisionWrapper>
              <div>
                <GenericHeader>
                  <SectionLabel>
                    {i18n.decisionDraft.reasonings.generic}
                  </SectionLabel>
                </GenericHeader>
                {generic.reasoning ? (
                  <GenericCard
                    $notReady={!generic.reasoning.ready}
                    data-qa={`generic-card-${generic.collectionType}`}
                  >
                    {!generic.reasoning.ready && (
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
                      {generic.validUntil
                        ? i18n.decisionDraft.reasonings.genericRangeClosed(
                            generic.reasoning.validFrom.format(),
                            generic.validUntil.format()
                          )
                        : i18n.decisionDraft.reasonings.genericRangeOpen(
                            generic.reasoning.validFrom.format()
                          )}
                    </GenericTitle>
                    <span>{text(generic.reasoning)}</span>
                  </GenericCard>
                ) : (
                  <Muted>
                    {i18n.decisionDraft.reasonings.noGenericForSlot}
                  </Muted>
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
                {preview.individualReasonings.length === 0 ? (
                  <Muted>{i18n.decisionDraft.reasonings.noIndividual}</Muted>
                ) : (
                  <FixedSpaceColumn $spacing="s">
                    {preview.individualReasonings.map((ind) => (
                      <IndividualCard
                        key={ind.id}
                        data-qa={`individual-card-${ind.id}`}
                      >
                        <IndividualTitle>{title(ind)}</IndividualTitle>
                        <span>{text(ind)}</span>
                      </IndividualCard>
                    ))}
                  </FixedSpaceColumn>
                )}
              </div>

              {pickerOpen && (
                <IndividualReasoningPickerModal
                  decisionId={decision.id}
                  decisionType={decision.type}
                  childName={childName}
                  decisionTypeLabel={typeLabel}
                  linkedIds={linkedIds}
                  onClose={() => setPickerOpen(false)}
                />
              )}
            </DecisionWrapper>
          )
        })}
      </Body>
    </Card>
  )
})
