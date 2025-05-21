// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { ContactInfoFormData } from 'lib-common/api-types/application/ApplicationFormData'
import type { UpdateStateFn } from 'lib-common/form-state'
import { getErrorCount } from 'lib-common/form-validation'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'

import { useTranslation } from '../../../localization'
import EditorSection from '../EditorSection'
import type { ApplicationFormDataErrors } from '../validations'

import ChildSubSection from './ChildSubSection'
import GuardianSubSection from './GuardianSubSection'
import OtherChildrenSubSection from './OtherChildrenSubSection'
import OtherPartnerSubSection from './OtherPartnerSubSection'
import SecondGuardianSubSection from './SecondGuardianSubSection'

export type ContactInfoSectionProps = {
  type: ApplicationType
  formData: ContactInfoFormData
  updateFormData: UpdateStateFn<ContactInfoFormData>
  errors: ApplicationFormDataErrors['contactInfo']
  verificationRequested: boolean
  fullFamily: boolean
  otherGuardianStatus: 'NO' | 'SAME_ADDRESS' | 'DIFFERENT_ADDRESS'
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
      {props.type !== 'CLUB' && (
        <>
          <HorizontalLine />
          <SecondGuardianSubSection {...props} />
        </>
      )}
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
