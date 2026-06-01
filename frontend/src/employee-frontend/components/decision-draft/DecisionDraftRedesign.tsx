// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import { combine } from 'lib-common/api'
import type { DecisionDraftGroup } from 'lib-common/generated/api-types/application'
import type {
  DecisionDraft,
  DecisionType,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faEnvelope } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'

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

const FooterRow = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: ${defaultMargins.XL};
  gap: ${defaultMargins.s};
`

const InlineWarning = styled.div`
  color: ${colors.status.warning};
`

function redirectToMainPage(navigate: (location: string) => void) {
  navigate('/applications')
}

function isUnitDataComplete(
  decision: DecisionDraft,
  units: DecisionUnit[],
  isClubDecision: boolean
) {
  const requiresDaycareDecisionName =
    decisionTypesRequiringDaycareDecisionName.includes(decision.type)
  const requiresPreschoolDecisionName =
    decisionTypesRequiringPreschoolDecisionName.includes(decision.type)
  const u = units.find((o) => o.id === decision.unitId)
  return (
    !!u &&
    !!(!requiresDaycareDecisionName || u.daycareDecisionName) &&
    !!(!requiresPreschoolDecisionName || u.preschoolDecisionName) &&
    !!u.manager &&
    !!u.streetAddress &&
    !!u.postalCode &&
    !!u.postOffice &&
    (isClubDecision || (!!u.decisionHandler && !!u.decisionHandlerAddress))
  )
}

export default React.memo(function DecisionDraftRedesign() {
  const applicationId = useIdRouteParam<ApplicationId>('id')
  const draftGroupResult = useQueryResult(
    decisionDraftsQuery({ applicationId })
  )
  const unitsResult = useQueryResult(decisionUnitsQuery())

  return renderResult(
    combine(draftGroupResult, unitsResult),
    ([draftGroup, units]) => (
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

    const isClubDecision = primaryDecision.type === 'CLUB'
    const primaryUnitDataIsComplete = useMemo(() => {
      return isUnitDataComplete(primaryDecision, units, isClubDecision)
    }, [primaryDecision, units, isClubDecision])
    const connectedUnitDataIsComplete = useMemo(() => {
      return connectedDecision
        ? isUnitDataComplete(connectedDecision, units, isClubDecision)
        : true
    }, [connectedDecision, units, isClubDecision])
    // TODO: Currently if the two decisions have different units, we don't really show which one of them has incomplete data.
    //  Also in this new design we no longer display all the data that can be missing.
    //  This should be discussed with the designer.
    const dataIncomplete =
      !primaryUnitDataIsComplete || !connectedUnitDataIsComplete

    const noDecisionsPlanned =
      connectedDecision !== null &&
      !primaryDecision.planned &&
      !connectedDecision.planned
    const childName = formatPersonName(child, 'First Last')

    return (
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
            showUnitSelector={!featureFlags.decisionDraftMultipleUnits}
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
              units={units}
              perDecisionUnitSelector={featureFlags.decisionDraftMultipleUnits}
              onPlannedChange={(planned) => updateState('primary', { planned })}
              onUnitChange={(unitId) => updateState('primary', { unitId })}
            />
            {connectedDecision && (
              <DecisionCard
                decision={connectedDecision}
                primaryDecisionType={primaryDecision.type}
                showPlannedCheckbox={true}
                childName={childName}
                units={units}
                perDecisionUnitSelector={
                  featureFlags.decisionDraftMultipleUnits
                }
                onPlannedChange={(planned) =>
                  updateState('connected', { planned })
                }
                onUnitChange={(unitId) => updateState('connected', { unitId })}
              />
            )}
          </DecisionCardWrapper>

          <FooterRow>
            {dataIncomplete && (
              <InlineWarning>
                {i18n.decisionDraft.unitDataMissing}
              </InlineWarning>
            )}
            <FixedSpaceRow>
              <Button
                onClick={() => redirectToMainPage(navigate)}
                text={i18n.common.cancel}
                data-qa="cancel-decisions-button"
              />
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
                    planned: d.planned
                  }))
                })}
                onSuccess={() => redirectToMainPage(navigate)}
                text={i18n.common.save}
              />
            </FixedSpaceRow>
          </FooterRow>
        </ContentArea>
      </Container>
    )
  }
)
