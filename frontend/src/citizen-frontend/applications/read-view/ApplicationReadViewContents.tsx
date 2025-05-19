// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import type { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import type { ApplicationDetails as ApplicationDetailsGen } from 'lib-common/generated/api-types/application'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Main from 'lib-components/atoms/Main'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1 } from 'lib-components/typography'

import { AuthContext } from '../../auth/state'
import { useTranslation } from '../../localization'
import AdditionalDetailsSection from '../editor/verification/AdditionalDetailsSection'
import BasicsSection from '../editor/verification/BasicsSection'
import ContactInfoSection from '../editor/verification/ContactInfoSection'
import ServiceNeedSection from '../editor/verification/ServiceNeedSection'
import UnitPreferenceSection from '../editor/verification/UnitPreferenceSection'

type DaycareApplicationReadViewProps = {
  application: ApplicationDetailsGen
  formData: ApplicationFormData
}

export default React.memo(function ApplicationReadViewContents({
  application,
  formData
}: DaycareApplicationReadViewProps) {
  const t = useTranslation()
  const type = application.type
  const { user } = useContext(AuthContext)

  const userIsApplicationGuardian = user
    .map((u) => u?.id === application.guardianId)
    .getOrElse(false)

  return (
    <Container>
      <ReturnButton label={t.common.return} />
      <Main>
        <ContentArea opaque>
          <H1>{t.applications.editor.heading.title[type]}</H1>
          <BasicsSection application={application} formData={formData} />
          <HorizontalLine />
          <ServiceNeedSection
            formData={formData}
            type={type}
            userIsApplicationGuardian={userIsApplicationGuardian}
          />
          <HorizontalLine />
          <UnitPreferenceSection formData={formData.unitPreference} />
          <HorizontalLine />
          {userIsApplicationGuardian && (
            <>
              <ContactInfoSection
                data-qa="contact-info-section"
                formData={formData.contactInfo}
                type={type}
                showFridgeFamilySection={
                  type === 'DAYCARE' ||
                  (type === 'PRESCHOOL' &&
                    formData.serviceNeed.connectedDaycare)
                }
              />
              <HorizontalLine />
              <AdditionalDetailsSection
                formData={formData}
                showAllergiesAndDiet={type !== 'CLUB'}
              />
            </>
          )}
        </ContentArea>
      </Main>
    </Container>
  )
})
