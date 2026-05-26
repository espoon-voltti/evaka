// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type {
  DecisionDraft,
  DecisionDraftUpdate,
  DecisionType,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
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

export default React.memo(function DecisionDraftRedesign() {
  const applicationId = useIdRouteParam<ApplicationId>('id')
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const draftGroupResult = useQueryResult(
    decisionDraftsQuery({ applicationId })
  )
  const unitsResult = useQueryResult(decisionUnitsQuery())
  const { mutateAsync: updateDecisionDrafts } = useMutationResult(
    updateDecisionDraftsMutation
  )
  const [decisions, setDecisions] = useState<DecisionDraft[]>([])
  const initialized = useRef(false)

  useEffect(() => {
    initialized.current = false
  }, [applicationId])

  useEffect(() => {
    if (draftGroupResult.isSuccess && !initialized.current) {
      initialized.current = true
      setDecisions(draftGroupResult.value.decisions)
    }
    if (draftGroupResult.isFailure && draftGroupResult.statusCode === 409) {
      redirectToMainPage(navigate)
    }
  }, [draftGroupResult, navigate])

  useTitle(
    draftGroupResult.map(
      (g) =>
        `${formatPersonName(g.child, 'Last First')} | ${i18n.titles.decision}`
    )
  )

  const updateState = (
    type: DecisionType | null,
    values: Partial<DecisionDraft>
  ) =>
    setDecisions((prev) =>
      prev.map((d) =>
        type === null || d.type === type ? { ...d, ...values } : d
      )
    )

  const unitOptions = useMemo(
    (): DecisionUnit[] => unitsResult.getOrElse([]),
    [unitsResult]
  )

  return (
    <Container>
      <ContentArea $opaque>
        {renderResult(draftGroupResult, (group) => {
          const isClubDecision = decisions.some(({ type }) => type === 'CLUB')
          const requiresDaycareDecisionName = decisions.some(({ type }) =>
            decisionTypesRequiringDaycareDecisionName.includes(type)
          )
          const requiresPreschoolDecisionName = decisions.some(({ type }) =>
            decisionTypesRequiringPreschoolDecisionName.includes(type)
          )
          const unitDataIsComplete = decisions.map((d) => {
            const u = unitOptions.find((o) => o.id === d.unitId)
            return (
              !!u &&
              !!(!requiresDaycareDecisionName || u.daycareDecisionName) &&
              !!(!requiresPreschoolDecisionName || u.preschoolDecisionName) &&
              !!u.manager &&
              !!u.streetAddress &&
              !!u.postalCode &&
              !!u.postOffice &&
              (isClubDecision ||
                (!!u.decisionHandler && !!u.decisionHandlerAddress))
            )
          })
          const noDecisionsPlanned =
            decisions.filter((d) => d.planned).length === 0
          const dataIncomplete = unitDataIsComplete.some((c) => !c)
          const sharedUnitId: DaycareId | null = decisions[0]?.unitId ?? null
          const childName = formatPersonName(group.child, 'First Last')

          return (
            <>
              <Title size={1}>
                {decisions.length > 1
                  ? i18n.decisionDraft.titlePlural
                  : i18n.decisionDraft.titleSingle}
              </Title>
              <Gap $size="xs" />
              <ApplicationHeaderCard
                child={group.child}
                guardian={group.guardian}
                otherGuardian={group.otherGuardian}
                placementUnitName={group.placementUnitName}
                units={unitOptions}
                selectedUnitId={sharedUnitId}
                onSelectUnit={(unitId) => updateState(null, { unitId })}
                showUnitSelector={!featureFlags.decisionDraftMultipleUnits}
              />

              {!(group.guardian.ssn && group.child.ssn) && (
                <InfoBox
                  title={i18n.decisionDraft.ssnInfo1}
                  message={i18n.decisionDraft.ssnInfo2}
                  icon={faEnvelope}
                />
              )}
              {!group.guardian.isVtjGuardian && (
                <AlertBox
                  title={i18n.decisionDraft.notGuardianInfo1}
                  message={i18n.decisionDraft.notGuardianInfo2}
                />
              )}

              <SectionHeading>
                <Title size={2}>
                  {decisions.length > 1
                    ? i18n.decisionDraft.decisionsHeading
                    : i18n.decisionDraft.decisionsHeadingSingle}
                </Title>
                {decisions.length > 1 && (
                  <span>{i18n.decisionDraft.decisionsSubtitle}</span>
                )}
              </SectionHeading>

              <DecisionCardWrapper>
                {decisions.map((d) => (
                  <DecisionCard
                    key={d.id}
                    decision={d}
                    decisions={decisions}
                    childName={childName}
                    units={unitOptions}
                    perDecisionUnitSelector={
                      featureFlags.decisionDraftMultipleUnits === true
                    }
                    onPlannedChange={(planned) =>
                      updateState(d.type, { planned })
                    }
                    onUnitChange={(unitId) => updateState(d.type, { unitId })}
                  />
                ))}
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
                  <AsyncButton
                    primary
                    data-qa="save-decisions-button"
                    disabled={dataIncomplete || noDecisionsPlanned}
                    onClick={() => {
                      const updates: DecisionDraftUpdate[] = decisions.map(
                        (d) => ({
                          id: d.id,
                          unitId: d.unitId,
                          startDate: d.startDate,
                          endDate: d.endDate,
                          planned: d.planned
                        })
                      )
                      return updateDecisionDrafts({
                        applicationId,
                        body: updates
                      })
                    }}
                    onSuccess={() => redirectToMainPage(navigate)}
                    text={i18n.common.save}
                  />
                </FixedSpaceRow>
              </FooterRow>
            </>
          )
        })}
      </ContentArea>
    </Container>
  )
})
