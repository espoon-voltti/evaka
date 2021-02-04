// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Container, {
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import { H1, P } from '@evaka/lib-components/src/typography'
import { useTranslation } from '~localization'
import BasicsSection from '~applications/editor/verification/BasicsSection'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import UnitPreferenceSection from '~applications/editor/verification/UnitPreferenceSection'
import { ApplicationDetails } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import ServiceNeedSection from './ServiceNeedSection'
import { espooBrandColors } from '@evaka/lib-components/src/colors'
import styled from 'styled-components'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { faInfo } from '@evaka/lib-icons'
import ContactInfoSection from './ContactInfoSection'
import { ApplicationType } from '~../../lib-common/src/api-types/application/enums'
import AdditionalDetailsSection from './AdditionalDetailsSection'

type DaycareApplicationVerificationViewProps = {
  application: ApplicationDetails
  formData: ApplicationFormData
  type: ApplicationType
  closeVerification: () => void
}

const AttachmentBox = styled.div`
  border: 2px solid ${espooBrandColors.espooTurquoise};
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
  return (
    <Container>
      <ContentArea opaque>
        <H1>{t.applications.editor.verification.title[type]}</H1>
        <P
          dangerouslySetInnerHTML={{
            __html: t.applications.editor.verification.notYetSent
          }}
        />
        {(formData.serviceNeed.urgent &&
          formData.serviceNeed.urgencyAttachments.length === 0) ||
          (formData.serviceNeed.shiftCare &&
            formData.serviceNeed.shiftCareAttachments.length === 0 && (
              <AttachmentBox>
                <RoundIconStyled
                  content={faInfo}
                  color={espooBrandColors.espooTurquoise}
                  size="s"
                />
                <div>
                  <P>
                    <strong>
                      {t.applications.editor.verification.attachmentBox.nb}
                    </strong>
                    {t.applications.editor.verification.attachmentBox.headline}
                  </P>
                  <ul>
                    {formData.serviceNeed.urgent &&
                      formData.serviceNeed.urgencyAttachments.length === 0 && (
                        <li>
                          {
                            t.applications.editor.verification.attachmentBox
                              .urgency
                          }
                        </li>
                      )}
                    {formData.serviceNeed.shiftCare &&
                      formData.serviceNeed.shiftCareAttachments.length ===
                        0 && (
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
                    </a>
                    {
                      t.applications.editor.verification.attachmentBox
                        .goBackRestText
                    }
                  </P>
                </div>
              </AttachmentBox>
            ))}
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
        <AdditionalDetailsSection
          formData={formData}
          showAllergiesAndDiet={type !== 'CLUB'}
        />
      </ContentArea>
    </Container>
  )
})
