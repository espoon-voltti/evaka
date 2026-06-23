// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import type { DecisionDraftGroup } from 'lib-common/generated/api-types/application'
import type {
  DecisionDraft,
  DecisionType,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faEnvelope } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'
import StickyActionBar from '../common/StickyActionBar'

import ApplicationHeaderCard from './ApplicationHeaderCard'
import DecisionCard from './DecisionCard'
import {
  decisionDraftsQuery,
  decisionUnitsQuery,
  updateDecisionDraftsMutation
} from './queries'

const decisionTypesRequiringDaycareDecisionName: DecisionType[] = [
  'DAYCARE',
  'DAYCARE_PART_TIME'
]

const decisionTypesRequiringPreschoolDecisionName: DecisionType[] = [
  'PRESCHOOL',
  'PREPARATORY_EDUCATION',
  'PRESCHOOL_DAYCARE',
  'PRESCHOOL_CLUB'
]

const SectionHeading = styled.div`
  margin-bottom: ${defaultMargins.xs};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xxs};
`

const DecisionCardWrapper = styled.div`
  display: flex;
  flex-direction: row;
  margin-top: ${defaultMargins.m};
  gap: ${defaultMargins.m};

  & > * {
    flex: 1;
  }
`

function redirectToMainPage(navigate: (location: string) => void) {
  navigate('/applications')
}

type DecisionUnitField =
  | 'unit'
  | 'daycareDecisionName'
  | 'preschoolDecisionName'
  | 'manager'
  | 'streetAddress'
  | 'postalCode'
  | 'postOffice'
  | 'decisionHandler'
  | 'decisionHandlerAddress'

function getMissingUnitFields(
  decision: DecisionDraft,
  units: DecisionUnit[],
  isClubDecision: boolean
): DecisionUnitField[] {
  const u = units.find((o) => o.id === decision.unitId)
  if (!u) return ['unit']

  const requiresDaycareDecisionName =
    decisionTypesRequiringDaycareDecisionName.includes(decision.type)
  const requiresPreschoolDecisionName =
    decisionTypesRequiringPreschoolDecisionName.includes(decision.type)

  const missing: DecisionUnitField[] = []
  if (requiresDaycareDecisionName && !u.daycareDecisionName)
    missing.push('daycareDecisionName')
  if (requiresPreschoolDecisionName && !u.preschoolDecisionName)
    missing.push('preschoolDecisionName')
  if (!u.manager) missing.push('manager')
  if (!u.streetAddress) missing.push('streetAddress')
  if (!u.postalCode) missing.push('postalCode')
  if (!u.postOffice) missing.push('postOffice')
  if (!isClubDecision) {
    if (!u.decisionHandler) missing.push('decisionHandler')
    if (!u.decisionHandlerAddress) missing.push('decisionHandlerAddress')
  }
  return missing
}

export default React.memo(function DecisionDraftRedesign() {
  const applicationId = useIdRouteParam<ApplicationId>('id')
  const draftGroupResult = useQueryResult(
    decisionDraftsQuery({ applicationId }),
    {
      refetchOnWindowFocus: false,
      refetchOnReconnect: false
    }
  )
  const unitsResult = useQueryResult(decisionUnitsQuery(), {
    refetchOnWindowFocus: false,
    refetchOnReconnect: false
  })

  return renderResult(
    combine(draftGroupResult, unitsResult),
    ([draftGroup, units], isReloading) =>
      isReloading ? null : (
        <DecisionDraftRedesignInner
          applicationId={applicationId}
          decisionDraftGroup={draftGroup}
          units={units}
        />
      )
  )
})

const DecisionDraftRedesignInner = React.memo(
  function DecisionDraftRedesignInner({
    applicationId,
    decisionDraftGroup,
    units
  }: {
    applicationId: ApplicationId
    decisionDraftGroup: DecisionDraftGroup
    units: DecisionUnit[]
  }) {
    const { i18n } = useTranslation()
    const { featureConfig } = useContext(UserContext)
    const svEnabled =
      featureConfig?.placementDecisionSwedishLanguageEnabled ?? false
    const [, navigate] = useLocation()

    const {
      child,
      primaryDecision: initialPrimaryDecision,
      connectedDecision: initialConnectedDecision,
      guardian,
      otherGuardian,
      placementUnit
    } = decisionDraftGroup

    const [primaryDecision, setPrimaryDecision] = useState<DecisionDraft>(
      initialPrimaryDecision
    )
    const [connectedDecision, setConnectedDecision] =
      useState<DecisionDraft | null>(initialConnectedDecision)

    useTitle(
      `${formatPersonName(child, 'Last First')} | ${i18n.titles.decision}`
    )

    const updateState = (
      type: 'primary' | 'connected' | 'both',
      values: Partial<DecisionDraft>
    ) => {
      if (type === 'primary' || type === 'both') {
        setPrimaryDecision((prev) => ({ ...prev, ...values }))
      }
      if (type === 'connected' || type === 'both') {
        setConnectedDecision((prev) => (prev ? { ...prev, ...values } : null))
      }
    }

    const decisionLanguage = (unitId: DaycareId): 'fi' | 'sv' =>
      units.find((u) => u.id === unitId)?.language === 'sv' ? 'sv' : 'fi'
    const misconfigured = (unitId: DaycareId) =>
      decisionLanguage(unitId) === 'sv' && !svEnabled

    const isClubDecision = primaryDecision.type === 'CLUB'
    const incompleteUnit = useMemo(() => {
      const decisions = [
        primaryDecision,
        ...(connectedDecision ? [connectedDecision] : [])
      ]
      const fields = new Set<DecisionUnitField>()
      for (const decision of decisions) {
        getMissingUnitFields(decision, units, isClubDecision).forEach((field) =>
          fields.add(field)
        )
      }
      if (fields.size === 0) return null
      const unit = units.find((u) => u.id === primaryDecision.unitId)
      return { unitName: unit?.name ?? null, fields: [...fields] }
    }, [primaryDecision, connectedDecision, units, isClubDecision])
    const dataIncomplete = incompleteUnit !== null

    const noDecisionsPlanned =
      connectedDecision !== null &&
      !primaryDecision.planned &&
      !connectedDecision.planned
    const childName = formatPersonName(child, 'First Last')

    return (
      <>
        <Container>
          <ContentArea $opaque>
            <Title size={1}>
              {connectedDecision !== null
                ? i18n.decisionDraft.titlePlural
                : i18n.decisionDraft.titleSingle}
            </Title>
            <Gap $size="xs" />
            <ApplicationHeaderCard
              child={child}
              guardian={guardian}
              otherGuardian={otherGuardian}
              placementUnitName={placementUnit.name}
              units={units}
              selectedUnitId={primaryDecision.unitId}
              onSelectUnit={(unitId) => updateState('both', { unitId })}
            />

            {!(guardian.ssn && child.ssn) && (
              <InfoBox
                title={i18n.decisionDraft.ssnInfo1}
                message={i18n.decisionDraft.ssnInfo2}
                icon={faEnvelope}
              />
            )}
            {!guardian.isVtjGuardian && (
              <AlertBox
                title={i18n.decisionDraft.notGuardianInfo1}
                message={i18n.decisionDraft.notGuardianInfo2}
              />
            )}

            <SectionHeading>
              <Title size={2}>
                {connectedDecision !== null
                  ? i18n.decisionDraft.decisionsHeading
                  : i18n.decisionDraft.decisionsHeadingSingle}
              </Title>
              {connectedDecision !== null && (
                <span>{i18n.decisionDraft.decisionsSubtitle}</span>
              )}
            </SectionHeading>

            <DecisionCardWrapper>
              <DecisionCard
                decision={primaryDecision}
                primaryDecisionType={primaryDecision.type}
                showPlannedCheckbox={connectedDecision !== null}
                childName={childName}
                language={decisionLanguage(primaryDecision.unitId)}
                unitLanguageUnsupported={misconfigured(primaryDecision.unitId)}
                onPlannedChange={(planned) =>
                  updateState('primary', { planned })
                }
                onReasoningIdsChange={(ids) =>
                  updateState('primary', {
                    individualReasoningIds: Array.from(ids)
                  })
                }
              />
              {connectedDecision && (
                <DecisionCard
                  decision={connectedDecision}
                  primaryDecisionType={primaryDecision.type}
                  showPlannedCheckbox={true}
                  childName={childName}
                  language={decisionLanguage(connectedDecision.unitId)}
                  unitLanguageUnsupported={misconfigured(
                    connectedDecision.unitId
                  )}
                  onPlannedChange={(planned) =>
                    updateState('connected', { planned })
                  }
                  onReasoningIdsChange={(ids) =>
                    updateState('connected', {
                      individualReasoningIds: Array.from(ids)
                    })
                  }
                />
              )}
            </DecisionCardWrapper>

            {incompleteUnit !== null && (
              <AlertBox
                wide
                title={i18n.decisionDraft.unitInfo1}
                message={
                  <FixedSpaceColumn $spacing="s">
                    <div>
                      <strong>
                        {incompleteUnit.unitName ??
                          i18n.decisionDraft.unitFieldsMissingUnitName}
                      </strong>
                      <Gap $size="xs" />
                      <UnorderedList $spacing="xxs">
                        {incompleteUnit.fields.map((field) => (
                          <li key={field}>
                            {i18n.decisionDraft.unitFields[field]}
                          </li>
                        ))}
                      </UnorderedList>
                    </div>
                    <div>{i18n.decisionDraft.unitInfo2}</div>
                  </FixedSpaceColumn>
                }
              />
            )}
          </ContentArea>
        </Container>
        <StickyActionBar align="right">
          <Button
            onClick={() => redirectToMainPage(navigate)}
            text={i18n.common.cancel}
            data-qa="cancel-decisions-button"
          />
          <Gap $size="s" $horizontal />
          <MutateButton
            primary
            data-qa="save-decisions-button"
            disabled={dataIncomplete || noDecisionsPlanned}
            mutation={updateDecisionDraftsMutation}
            onClick={() => ({
              applicationId,
              body: [
                primaryDecision,
                ...(connectedDecision ? [connectedDecision] : [])
              ].map((d) => ({
                id: d.id,
                unitId: d.unitId,
                startDate: d.startDate,
                endDate: d.endDate,
                planned: d.planned,
                individualReasoningIds: misconfigured(d.unitId)
                  ? []
                  : d.individualReasoningIds
              }))
            })}
            onSuccess={() => redirectToMainPage(navigate)}
            text={i18n.common.save}
          />
        </StickyActionBar>
      </>
    )
  }
)
