// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import {
  ApplicationDetails as ApplicationDetailsGen,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faInfo } from 'lib-icons'

import BasicsSection from '../../../applications/editor/verification/BasicsSection'
import UnitPreferenceSection from '../../../applications/editor/verification/UnitPreferenceSection'
import { useTranslation } from '../../../localization'

import AdditionalDetailsSection from './AdditionalDetailsSection'
import ContactInfoSection from './ContactInfoSection'
import ServiceNeedSection from './ServiceNeedSection'

type DaycareApplicationVerificationViewProps = {
  application: ApplicationDetailsGen
  formData: ApplicationFormData
  type: ApplicationType
  closeVerification: () => void
}

const AttachmentBox = styled.div`
  border: 2px solid ${colors.main.m3};
  padding: 0 ${defaultMargins.m};
  display: flex;
`

const RoundIconStyled = styled(RoundIcon)`
  margin: ${defaultMargins.m} ${defaultMargins.m} 0 0;
`

export default React.memo(function ApplicationVerificationViewDaycare({
  application,
  formData,
  type,
  closeVerification
}: DaycareApplicationVerificationViewProps) {
  const t = useTranslation()
  const missingUrgencyAttachments =
    formData.serviceNeed.urgent &&
    formData.serviceNeed.urgencyAttachments.length === 0 &&
    featureFlags.urgencyAttachments

  const missingAttachments =
    missingUrgencyAttachments ||
    (formData.serviceNeed.shiftCare &&
      formData.serviceNeed.shiftCareAttachments.length === 0)
  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.applications.editor.verification.title[type]}</H1>
        {application.status === 'CREATED'
          ? t.applications.editor.verification.notYetSent
          : t.applications.editor.verification.notYetSaved}
        {missingAttachments && (
          <AttachmentBox>
            <RoundIconStyled content={faInfo} color={colors.main.m1} size="s" />
            <div>
              <P>
                <strong>
                  {t.applications.editor.verification.attachmentBox.nb}
                </strong>{' '}
                {t.applications.editor.verification.attachmentBox.headline}
              </P>
              <ul>
                {missingUrgencyAttachments && (
                  <li>
                    {t.applications.editor.verification.attachmentBox.urgency}
                  </li>
                )}
                {formData.serviceNeed.shiftCare &&
                  formData.serviceNeed.shiftCareAttachments.length === 0 && (
                    <li>
                      {
                        t.applications.editor.verification.attachmentBox
                          .shiftCare
                      }
                    </li>
                  )}
              </ul>
              <P>
                <a href="#" onClick={closeVerification} role="button">
                  {
                    t.applications.editor.verification.attachmentBox
                      .goBackLinkText
                  }
                </a>{' '}
                {
                  t.applications.editor.verification.attachmentBox
                    .goBackRestText
                }
              </P>
            </div>
          </AttachmentBox>
        )}
      </ContentArea>

      <Gap size="m" />

      <ContentArea opaque>
        <BasicsSection application={application} formData={formData} />
        <HorizontalLine />
        <ServiceNeedSection formData={formData} type={type} />
        <HorizontalLine />
        <UnitPreferenceSection formData={formData.unitPreference} />
        <HorizontalLine />
        <ContactInfoSection
          formData={formData.contactInfo}
          type={type}
          showFridgeFamilySection={
            type === 'DAYCARE' ||
            (type === 'PRESCHOOL' && formData.serviceNeed.connectedDaycare)
          }
        />
        <HorizontalLine />

        <AdditionalDetailsSection
          formData={formData}
          showAllergiesAndDiet={type !== 'CLUB'}
        />
      </ContentArea>
    </Container>
  )
})
