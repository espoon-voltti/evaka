// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { AssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { AssistanceNeedDecisionStatusChip } from 'lib-components/assistance-need-decision/AssistanceNeedDecisionStatusChip'
import Button from 'lib-components/atoms/buttons/Button'
import Container, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { translations } from 'lib-customizations/employee'

import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import { assistanceNeedPreschoolDecisionQuery } from '../../queries'

const WidthLimiter = styled.div`
  max-width: 700px;
`

const SectionSpacer = styled(FixedSpaceColumn).attrs({ spacing: 'L' })``

const LabeledValue = styled(FixedSpaceColumn).attrs({ spacing: 'xs' })``

const StyledUl = styled.ul`
  margin-top: 0;
`

const pageTranslations = {
  FI: translations['fi'].childInformation.assistanceNeedPreschoolDecision,
  SV: translations['sv'].childInformation.assistanceNeedPreschoolDecision
}

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

const DecisionReadView = React.memo(function DecisionReadView({
  decision
}: {
  decision: AssistanceNeedPreschoolDecision
}) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const t = pageTranslations[decision.form.language]

  return (
    <div>
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
                  {decision.decisionMakerName},{' '}
                  {decision.form.decisionMakerTitle}
                </span>
              </LabeledValue>
            </SectionSpacer>
          </FixedSpaceColumn>
        </ContentArea>
      </Container>

      <Gap size="m" />

      <StickyFooter>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <FixedSpaceRow alignItems="center">
            <Button
              text={i18n.childInformation.assistanceNeedDecision.leavePage}
              onClick={() =>
                navigate(`/child-information/${decision.child.id}`)
              }
              data-qa="leave-page-button"
            />
            <Button
              text={i18n.childInformation.assistanceNeedDecision.modifyDecision}
              onClick={() =>
                navigate(
                  `/child-information/${decision.child.id}/assistance-need-preschool-decisions/${decision.id}/edit`
                )
              }
              data-qa="edit-button"
            />
          </FixedSpaceRow>

          <Button
            primary
            text={
              i18n.childInformation.assistanceNeedDecision.sendToDecisionMaker
            }
            disabled={true}
            onClick={() => alert('todo')}
            data-qa="send-decision"
          />
        </FixedSpaceRow>
      </StickyFooter>
    </div>
  )
})

export default React.memo(function AssistanceNeedPreschoolDecisionReadPage() {
  const { decisionId } = useNonNullableParams<{ decisionId: UUID }>()
  const decisionResult = useQueryResult(
    assistanceNeedPreschoolDecisionQuery(decisionId)
  )

  return renderResult(decisionResult, (decisionResponse) => (
    <DecisionReadView decision={decisionResponse.decision} />
  ))
})
