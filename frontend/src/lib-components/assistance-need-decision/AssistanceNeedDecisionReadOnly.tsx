// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import some from 'lodash/some'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  AssistanceLevel,
  AssistanceNeedDecision
} from 'lib-common/generated/api-types/assistanceneed'
import { tabletMin } from 'lib-components/breakpoints'
import {
  CollapsibleContentArea,
  ContentArea
} from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H1, H2, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import AssistanceNeedDecisionInfoHeader from './AssistanceNeedDecisionInfoHeader'

export interface AssistanceNeedDecisionTexts {
  pageTitle: string
  annulmentReason: string
  neededTypesOfAssistance: string
  pedagogicalMotivation: string
  structuralMotivation: string
  structuralMotivationOptions: {
    smallerGroup: string
    specialGroup: string
    smallGroup: string
    groupAssistant: string
    childAssistant: string
    additionalStaff: string
  }
  careMotivation: string
  serviceOptions: {
    consultationSpecialEd: string
    partTimeSpecialEd: string
    fullTimeSpecialEd: string
    interpretationAndAssistanceServices: string
    specialAides: string
  }
  services: string
  collaborationWithGuardians: string
  guardiansHeardOn: string
  guardiansHeard: string
  viewOfTheGuardians: string
  decisionAndValidity: string
  futureLevelOfAssistance: string
  assistanceLevel: {
    assistanceEnds: string
    assistanceServicesForTime: string
    enhancedAssistance: string
    specialAssistance: string
  }
  startDate: string
  endDate: string
  endDateServices: string
  selectedUnit: string
  unitMayChange: string
  motivationForDecision: string
  personsResponsible: string
  preparator: string
  decisionMaker: string
  disclaimer: string
  decisionNumber: string
  statuses: {
    DRAFT: string
    NEEDS_WORK: string
    ACCEPTED: string
    REJECTED: string
    ANNULLED: string
  }
  confidential: string
  lawReference: string
  appealInstructionsTitle: string
  appealInstructions: React.JSX.Element
  legalInstructions: string
  legalInstructionsText: string
  jurisdiction: string
  jurisdictionText: () => React.ReactNode
}

const List = styled.ul`
  list-style-position: inside;
  padding-left: 0;
  margin: ${defaultMargins.xs} 0;
`

const LabelContainer = styled.div`
  margin-bottom: ${defaultMargins.s};
`

const Value = styled.div`
  white-space: pre-line;
`

export const ParagraphDiv = styled.div`
  max-width: 960px;
  margin: 0;
`

const OptionalLabelledValue = React.memo(function OptionalLabelledValue({
  label,
  value,
  'data-qa': dataQa
}: {
  label: string
  value: React.ReactNode | null | undefined
  'data-qa'?: string
}) {
  if (!value) {
    return null
  }

  return (
    <LabelContainer>
      <Label>{label}</Label>
      <Gap size="xs" />
      <Value data-qa={`labelled-value-${dataQa ?? label}`}>{value}</Value>
    </LabelContainer>
  )
})

interface BoolObjectListProps<T, K extends keyof T> {
  obj: T
  texts: Record<K, string>
}

function BoolObjectList<K extends string, T extends Record<K, boolean>>({
  obj,
  texts
}: BoolObjectListProps<T, K>) {
  return (
    <List>
      {Object.entries(obj)
        .filter(
          (option): option is [K, true] =>
            typeof option[1] === 'boolean' && option[1] && option[0] in texts
        )
        .map(([option]) => (
          <li key={option} data-qa={`list-option-${option}`}>
            {texts[option]}
          </li>
        ))}
    </List>
  )
}

const someBoolObj = <K extends string>(obj: Record<K, boolean>) =>
  some(obj, (v) => v)

const TitleRow = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;

  @media (max-width: ${tabletMin}) {
    flex-direction: column-reverse;
    gap: ${defaultMargins.s};
  }
`

const ChildName = styled(H2).attrs({
  as: 'div'
})`
  color: ${(p) => p.theme.colors.grayscale.g100};
`

export default React.memo(function AssistanceNeedDecisionReadOnly({
  decision,
  decisionMakerWarning,
  texts: t
}: {
  decision: AssistanceNeedDecision
  decisionMakerWarning?: React.ReactNode
  texts: AssistanceNeedDecisionTexts
}) {
  const assistanceLevelTexts: Record<AssistanceLevel, string> = useMemo(
    () => ({
      ASSISTANCE_ENDS: t.assistanceLevel.assistanceEnds,
      ASSISTANCE_SERVICES_FOR_TIME: t.assistanceLevel.assistanceServicesForTime,
      ENHANCED_ASSISTANCE: t.assistanceLevel.enhancedAssistance,
      SPECIAL_ASSISTANCE: t.assistanceLevel.specialAssistance
    }),
    [t]
  )

  const [appealInstructionsOpen, setAppealInstructionsOpen] = useState(false)

  return (
    <>
      <ContentArea opaque>
        <TitleRow>
          <FixedSpaceColumn>
            <H1 noMargin data-qa="page-title">
              {t.pageTitle}
            </H1>
            <ChildName noMargin translate="no">
              {decision.child?.name}
            </ChildName>
          </FixedSpaceColumn>
          <AssistanceNeedDecisionInfoHeader
            decisionNumber={decision.decisionNumber ?? 0}
            decisionStatus={decision.status}
            texts={t}
          />
        </TitleRow>
        <Gap size="s" />
        <FixedSpaceColumn spacing={defaultMargins.s}>
          <OptionalLabelledValue
            label={t.annulmentReason}
            value={decision.annulmentReason}
            data-qa="annulment-reason"
          />

          <H2>{t.neededTypesOfAssistance}</H2>

          <div>
            <OptionalLabelledValue
              label={t.pedagogicalMotivation}
              value={decision.pedagogicalMotivation}
              data-qa="pedagogical-motivation"
            />

            {(someBoolObj(decision.structuralMotivationOptions) ||
              !!decision.structuralMotivationDescription) && (
              <LabelContainer data-qa="structural-motivation-section">
                <Label>{t.structuralMotivation}</Label>
                <BoolObjectList
                  obj={decision.structuralMotivationOptions}
                  texts={t.structuralMotivationOptions}
                />
                {!!decision.structuralMotivationDescription && (
                  <P
                    noMargin
                    preserveWhiteSpace
                    data-qa="structural-motivation-description"
                  >
                    {decision.structuralMotivationDescription}
                  </P>
                )}
              </LabelContainer>
            )}

            <OptionalLabelledValue
              label={t.careMotivation}
              value={decision.careMotivation}
              data-qa="care-motivation"
            />

            {(someBoolObj(decision.serviceOptions) ||
              !!decision.servicesMotivation) && (
              <div data-qa="services-section">
                <Label>{t.services}</Label>
                <BoolObjectList
                  obj={decision.serviceOptions}
                  texts={t.serviceOptions}
                />
                {!!decision.servicesMotivation && (
                  <P noMargin preserveWhiteSpace>
                    {decision.servicesMotivation}
                  </P>
                )}
              </div>
            )}
          </div>

          <H2>{t.collaborationWithGuardians}</H2>

          <div>
            <OptionalLabelledValue
              label={t.guardiansHeardOn}
              value={decision.guardiansHeardOn?.format()}
              data-qa="guardians-heard-at"
            />

            {(some(decision.guardianInfo, (i) => i.isHeard) ||
              decision.otherRepresentativeHeard) && (
              <LabelContainer data-qa="guardians-heard-section">
                <Label>{t.guardiansHeard}</Label>
                <List>
                  {decision.guardianInfo
                    .filter((g) => g.id !== null && g.isHeard)
                    .map((guardian) => (
                      <li
                        key={guardian.personId}
                        data-qa={`guardian-${guardian.personId ?? ''}`}
                      >
                        <span translate="no">{guardian.name}</span>:{' '}
                        {guardian.details}
                      </li>
                    ))}
                  {decision.otherRepresentativeHeard && (
                    <li data-qa="other-representative-details">
                      {decision.otherRepresentativeDetails}
                    </li>
                  )}
                </List>
              </LabelContainer>
            )}

            <OptionalLabelledValue
              label={t.viewOfTheGuardians}
              value={decision.viewOfGuardians}
              data-qa="view-of-the-guardians"
            />
          </div>

          <H2>{t.decisionAndValidity}</H2>

          <div>
            <OptionalLabelledValue
              label={t.futureLevelOfAssistance}
              value={decision.assistanceLevels
                .map((level) => assistanceLevelTexts[level])
                .join(', ')}
              data-qa="future-level-of-assistance"
            />

            <OptionalLabelledValue
              label={t.startDate}
              value={decision.validityPeriod.start.format()}
              data-qa="start-date"
            />

            <OptionalLabelledValue
              label={
                decision.assistanceLevels.includes(
                  'ASSISTANCE_SERVICES_FOR_TIME'
                )
                  ? t.endDateServices
                  : t.endDate
              }
              value={decision.validityPeriod.end?.format()}
              data-qa="end-date"
            />

            <OptionalLabelledValue
              label={t.selectedUnit}
              value={
                !!decision.selectedUnit?.id && (
                  <LabelContainer>
                    <div translate="no">{decision.selectedUnit?.name}</div>
                    <div translate="no">
                      {decision.selectedUnit?.streetAddress}
                    </div>
                    <div>
                      {decision.selectedUnit?.postalCode}{' '}
                      {decision.selectedUnit?.postOffice}
                    </div>
                    <Gap size="s" />
                    <div>{t.unitMayChange}</div>
                  </LabelContainer>
                )
              }
              data-qa="selected-unit"
            />

            <OptionalLabelledValue
              label={t.motivationForDecision}
              value={decision.motivationForDecision}
              data-qa="motivation-for-decision"
            />
          </div>

          <H2>{t.legalInstructions}</H2>
          <P noMargin>{t.legalInstructionsText}</P>

          <H2>{t.jurisdiction}</H2>
          <ParagraphDiv>{t.jurisdictionText()}</ParagraphDiv>

          <H2>{t.personsResponsible}</H2>

          <div>
            <OptionalLabelledValue
              label={t.preparator}
              value={
                !!decision.preparedBy1?.employeeId && (
                  <LabelContainer>
                    <div translate="no">
                      {decision.preparedBy1.name}, {decision.preparedBy1.title}
                    </div>
                    <div>{decision.preparedBy1.phoneNumber}</div>
                  </LabelContainer>
                )
              }
              data-qa="prepared-by-1"
            />

            <OptionalLabelledValue
              label={t.preparator}
              value={
                !!decision.preparedBy2?.employeeId && (
                  <LabelContainer>
                    <div translate="no">
                      {decision.preparedBy2.name}, {decision.preparedBy2.title}
                    </div>
                    <div>{decision.preparedBy2.phoneNumber}</div>
                  </LabelContainer>
                )
              }
              data-qa="prepared-by-2"
            />

            <OptionalLabelledValue
              label={t.decisionMaker}
              value={
                !!decision.decisionMaker?.employeeId && (
                  <LabelContainer translate="no">
                    {decision.decisionMaker.name},{' '}
                    {decision.decisionMaker.title}
                  </LabelContainer>
                )
              }
              data-qa="decision-maker"
            />
          </div>
        </FixedSpaceColumn>

        {decisionMakerWarning}

        <P>{t.disclaimer}</P>
      </ContentArea>

      <Gap size="m" />

      <CollapsibleContentArea
        title={<H2 noMargin>{t.appealInstructionsTitle}</H2>}
        open={appealInstructionsOpen}
        toggleOpen={() => setAppealInstructionsOpen(!appealInstructionsOpen)}
        opaque
      >
        {t.appealInstructions}
      </CollapsibleContentArea>
    </>
  )
})
