// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'
import styled from 'styled-components'

import { AssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'

import Container, {
  CollapsibleContentArea,
  ContentArea
} from '../layout/Container'
import { FixedSpaceColumn, FixedSpaceRow } from '../layout/flex-helpers'
import { H1, H2, Label } from '../typography'
import { Gap } from '../white-space'

import { AssistanceNeedDecisionStatusChip } from './AssistanceNeedDecisionStatusChip'

const WidthLimiter = styled.div`
  max-width: 700px;
`

const SectionSpacer = styled(FixedSpaceColumn).attrs({ spacing: 'L' })``

const LabeledValue = styled(FixedSpaceColumn).attrs({ spacing: 'xs' })``

const StyledUl = styled.ul`
  margin-top: 0;
`

const ReadOnlyTextArea = React.memo(function ReadOnlyTextArea({
  text
}: {
  text: string
}) {
  return (
    <WidthLimiter>
      {text.split('\n').map((line, i) => (
        <Fragment key={i}>
          {line}
          <br />
        </Fragment>
      ))}
    </WidthLimiter>
  )
})

interface ViewTranslations {
  statuses: {
    DRAFT: string
    NEEDS_WORK: string
    ACCEPTED: string
    REJECTED: string
    ANNULLED: string
  }
  pageTitle: string
  decisionNumber: string
  confidential: string
  lawReference: string
  types: {
    NEW: string
    CONTINUING: string
    TERMINATED: string
  }
  decidedAssistance: string
  type: string
  validFrom: string
  extendedCompulsoryEducationSection: string
  extendedCompulsoryEducation: string
  no: string
  extendedCompulsoryEducationInfo: string
  grantedAssistanceSection: string
  grantedAssistanceService: string
  grantedInterpretationService: string
  grantedAssistiveDevices: string
  grantedNothing: string
  grantedServicesBasis: string
  selectedUnit: string
  primaryGroup: string
  decisionBasis: string
  documentBasis: string
  basisDocumentPedagogicalReport: string
  basisDocumentPsychologistStatement: string
  basisDocumentDoctorStatement: string
  basisDocumentSocialReport: string
  basisDocumentOtherOrMissing: string
  basisDocumentsInfo: string
  guardianCollaborationSection: string
  guardiansHeardOn: string
  heardGuardians: string
  otherRepresentative: string
  viewOfGuardians: string
  responsiblePeople: string
  preparer: string
  decisionMaker: string
  employeeTitle: string
  phone: string
  appealInstructionsTitle: string
  appealInstructions: JSX.Element
}

export default React.memo(function DecisionFormReadView({
  decision,
  texts: t
}: {
  decision: AssistanceNeedPreschoolDecision
  texts: ViewTranslations
}) {
  const [appealInstructionsOpen, setAppealInstructionsOpen] = useState(false)
  return (
    <Container>
      <ContentArea opaque>
        <FixedSpaceRow justifyContent="space-between" alignItems="flex-start">
          <FixedSpaceColumn>
            <H1 noMargin>{t.pageTitle}</H1>
            <H2>{decision.child.name}</H2>
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <span>
              {t.decisionNumber} {decision.decisionNumber}
            </span>
            <AssistanceNeedDecisionStatusChip
              decisionStatus={decision.status}
              texts={t.statuses}
              data-qa="status"
            />
            <span>{t.confidential}</span>
            <span>{t.lawReference}</span>
          </FixedSpaceColumn>
        </FixedSpaceRow>
        <FixedSpaceColumn spacing="XL">
          <SectionSpacer>
            <H2>{t.decidedAssistance}</H2>

            <LabeledValue>
              <Label>{t.type}</Label>
              <span>
                {decision.form.type ? t.types[decision.form.type] : ''}
              </span>
            </LabeledValue>

            <LabeledValue>
              <Label>{t.validFrom}</Label>
              <span>{decision.form.validFrom?.format() ?? ''}</span>
            </LabeledValue>

            <LabeledValue>
              <Label>{t.extendedCompulsoryEducationSection}</Label>
              <StyledUl>
                <li>
                  {decision.form.extendedCompulsoryEducation
                    ? t.extendedCompulsoryEducation
                    : t.no}
                </li>
              </StyledUl>
              {decision.form.extendedCompulsoryEducation &&
                !!decision.form.extendedCompulsoryEducationInfo && (
                  <span>{decision.form.extendedCompulsoryEducationInfo}</span>
                )}
            </LabeledValue>

            <LabeledValue>
              <Label>{t.grantedAssistanceSection}</Label>
              {decision.form.grantedAssistanceService ||
              decision.form.grantedInterpretationService ||
              decision.form.grantedAssistiveDevices ? (
                <StyledUl>
                  {decision.form.grantedAssistanceService && (
                    <li>{t.grantedAssistanceService}</li>
                  )}
                  {decision.form.grantedInterpretationService && (
                    <li>{t.grantedInterpretationService}</li>
                  )}
                  {decision.form.grantedAssistiveDevices && (
                    <li>{t.grantedAssistiveDevices}</li>
                  )}
                </StyledUl>
              ) : (
                <span>{t.grantedNothing}</span>
              )}
            </LabeledValue>

            <LabeledValue>
              <Label>{t.grantedServicesBasis}</Label>
              <ReadOnlyTextArea
                text={decision.form.grantedServicesBasis || '-'}
              />
            </LabeledValue>

            <LabeledValue>
              <Label>{t.selectedUnit}</Label>
              <span>{decision.unitName ?? '-'}</span>
            </LabeledValue>

            <LabeledValue>
              <Label>{t.primaryGroup}</Label>
              <span>{decision.form.primaryGroup ?? '-'}</span>
            </LabeledValue>

            <LabeledValue>
              <Label>{t.decisionBasis}</Label>
              <ReadOnlyTextArea text={decision.form.decisionBasis} />
            </LabeledValue>

            <LabeledValue>
              <Label>{t.documentBasis}</Label>
              <StyledUl>
                {decision.form.basisDocumentPedagogicalReport && (
                  <li>{t.basisDocumentPedagogicalReport}</li>
                )}
                {decision.form.basisDocumentPsychologistStatement && (
                  <li>{t.basisDocumentPsychologistStatement}</li>
                )}
                {decision.form.basisDocumentSocialReport && (
                  <li>{t.basisDocumentSocialReport}</li>
                )}
                {decision.form.basisDocumentDoctorStatement && (
                  <li>{t.basisDocumentDoctorStatement}</li>
                )}
                {decision.form.basisDocumentOtherOrMissing && (
                  <li>{t.basisDocumentOtherOrMissing}</li>
                )}
              </StyledUl>
              {decision.form.basisDocumentOtherOrMissing &&
                !!decision.form.basisDocumentOtherOrMissingInfo && (
                  <ReadOnlyTextArea
                    text={decision.form.basisDocumentOtherOrMissingInfo}
                  />
                )}
            </LabeledValue>

            <LabeledValue>
              <Label>{t.basisDocumentsInfo}</Label>
              {decision.form.basisDocumentsInfo ? (
                <ReadOnlyTextArea text={decision.form.basisDocumentsInfo} />
              ) : (
                <span>-</span>
              )}
            </LabeledValue>
          </SectionSpacer>

          <SectionSpacer>
            <H2>{t.guardianCollaborationSection}</H2>

            <LabeledValue>
              <Label>{t.guardiansHeardOn}</Label>
              <span>{decision.form.guardiansHeardOn?.format() ?? '-'}</span>
            </LabeledValue>

            <FixedSpaceColumn>
              <Label>{t.heardGuardians}</Label>
              <StyledUl>
                {decision.form.guardianInfo
                  .filter((g) => g.isHeard)
                  .map((guardian) => (
                    <li
                      key={guardian.personId}
                    >{`${guardian.name}: ${guardian.details}`}</li>
                  ))}
                {decision.form.otherRepresentativeHeard && (
                  <li>
                    {t.otherRepresentative}: $
                    {decision.form.otherRepresentativeDetails}
                  </li>
                )}
              </StyledUl>
            </FixedSpaceColumn>

            <LabeledValue>
              <Label>{t.viewOfGuardians}</Label>
              <ReadOnlyTextArea text={decision.form.viewOfGuardians} />
            </LabeledValue>
          </SectionSpacer>

          <SectionSpacer>
            <H2>{t.responsiblePeople}</H2>

            <LabeledValue>
              <Label>{t.preparer}</Label>
              <span>
                {decision.preparer1Name}, {decision.form.preparer1Title}
              </span>
              {!!decision.form.preparer1PhoneNumber && (
                <span>{decision.form.preparer1PhoneNumber}</span>
              )}
            </LabeledValue>

            {!!decision.form.preparer2EmployeeId && (
              <LabeledValue>
                <Label>{t.preparer}</Label>
                <span>
                  {decision.preparer2Name}, {decision.form.preparer2Title}
                </span>
                {!!decision.form.preparer2PhoneNumber && (
                  <span>{decision.form.preparer2PhoneNumber}</span>
                )}
              </LabeledValue>
            )}

            <LabeledValue>
              <Label>{t.decisionMaker}</Label>
              <span>
                {decision.decisionMakerName}, {decision.form.decisionMakerTitle}
              </span>
            </LabeledValue>
          </SectionSpacer>
        </FixedSpaceColumn>
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
    </Container>
  )
})
