// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import some from 'lodash/some'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import {
  AssistanceLevel,
  AssistanceNeedDecision
} from 'lib-common/generated/api-types/assistanceneed'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { DecisionInfoHeader } from './common'

const List = styled.ul`
  list-style-position: inside;
  padding-left: 0;
  margin: ${defaultMargins.xs} 0;
`

const LabelContainer = styled.div`
  margin-bottom: ${defaultMargins.s};
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
      <div data-qa={`labelled-value-${dataQa ?? label}`}>{value}</div>
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

export default React.memo(function AssistanceNeedDecisionReadOnly({
  decision,
  decisionMakerWarning
}: {
  decision: AssistanceNeedDecision
  decisionMakerWarning?: React.ReactNode
}) {
  const {
    i18n: {
      childInformation: { assistanceNeedDecision: t },
      ...i18n
    }
  } = useTranslation()

  const assistanceLevelTexts: Record<AssistanceLevel, string> = useMemo(
    () => ({
      ASSISTANCE_ENDS: t.assistanceLevel.assistanceEnds,
      ASSISTANCE_SERVICES_FOR_TIME: `${
        t.assistanceLevel.assistanceServicesForTime
      } ${decision?.assistanceServicesTime?.format() ?? ''}`,
      ENHANCED_ASSISTANCE: t.assistanceLevel.enhancedAssistance,
      SPECIAL_ASSISTANCE: t.assistanceLevel.specialAssistance
    }),
    [t, decision]
  )

  return (
    <>
      <FixedSpaceRow
        alignItems="flex-start"
        justifyContent="space-between"
        fullWidth
      >
        <FixedSpaceColumn>
          <H1 noMargin>{i18n.titles.assistanceNeedDecision}</H1>
          <H2 noMargin>{decision.child?.name}</H2>
        </FixedSpaceColumn>
        <DecisionInfoHeader
          decisionNumber={decision.decisionNumber || 0}
          decisionStatus={decision.status || 'DRAFT'}
        />
      </FixedSpaceRow>
      <Gap size="s" />
      <FixedSpaceColumn spacing={defaultMargins.s}>
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
            label={t.guardiansHeardAt}
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
                      {guardian.name}: {guardian.details}
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
            value={
              decision.assistanceLevel &&
              assistanceLevelTexts[decision.assistanceLevel]
            }
            data-qa="future-level-of-assistance"
          />

          <OptionalLabelledValue
            label={t.startDate}
            value={decision.startDate?.format()}
            data-qa="start-date"
          />

          <OptionalLabelledValue
            label={t.selectedUnit}
            value={
              !!decision.selectedUnit?.id && (
                <LabelContainer>
                  <div>{decision.selectedUnit?.name}</div>
                  <div>{decision.selectedUnit?.streetAddress}</div>
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

        <H2>{t.personsResponsible}</H2>

        <div>
          <OptionalLabelledValue
            label={t.preparator}
            value={
              !!decision.preparedBy1?.employeeId && (
                <LabelContainer>
                  <div>
                    {decision.preparedBy1.name}, {decision.preparedBy1.title}
                  </div>
                  <div>{decision.preparedBy1.email}</div>
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
                  <div>
                    {decision.preparedBy2.name}, {decision.preparedBy2.title}
                  </div>
                  <div>{decision.preparedBy2.email}</div>
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
                <LabelContainer>
                  {decision.decisionMaker.name}, {decision.decisionMaker.title}
                </LabelContainer>
              )
            }
            data-qa="decision-maker"
          />

          {decisionMakerWarning}

          <P>{t.disclaimer}</P>
        </div>
      </FixedSpaceColumn>
    </>
  )
})
