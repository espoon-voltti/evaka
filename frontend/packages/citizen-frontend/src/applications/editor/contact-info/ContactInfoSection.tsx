// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~localization'
import { ContactInfoFormData } from '../ApplicationFormData'
import EditorSection from '~applications/editor/EditorSection'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import { ApplicationFormDataErrors } from '~applications/editor/validations'
import { getErrorCount } from '~form-validation'
import ChildSubSection from '~applications/editor/contact-info/ChildSubSection'
import GuardianSubSection from '~applications/editor/contact-info/GuardianSubSection'
import SecondGuardianSubSection from '~applications/editor/contact-info/SecondGuardianSubSection'
import OtherPartnerSubSection from '~applications/editor/contact-info/OtherPartnerSubSection'
import OtherChildrenSubSection from '~applications/editor/contact-info/OtherChildrenSubSection'

export type ContactInfoSectionProps = {
  formData: ContactInfoFormData
  updateFormData: (update: Partial<ContactInfoFormData>) => void
  errors: ApplicationFormDataErrors['contactInfo']
  verificationRequested: boolean
  fullFamily: boolean
}

export default React.memo(function ContactInfoSection(
  props: ContactInfoSectionProps
) {
  const t = useTranslation()

  return (
    <EditorSection
      title={t.applications.editor.contactInfo.title}
      validationErrors={
        props.verificationRequested ? getErrorCount(props.errors) : 0
      }
      data-qa="contactInfo-section"
    >
      <ChildSubSection {...props} />
      <HorizontalLine />
      <GuardianSubSection {...props} />
      <HorizontalLine />
      <SecondGuardianSubSection />
      {props.fullFamily && (
        <>
          <HorizontalLine />
          <OtherPartnerSubSection {...props} />
          <HorizontalLine />
          <OtherChildrenSubSection {...props} />
        </>
      )}
    </EditorSection>
  )
})
