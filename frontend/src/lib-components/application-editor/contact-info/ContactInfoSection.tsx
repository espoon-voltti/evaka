// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { ContactInfoFormData } from 'lib-common/application/ApplicationFormData'
import type { ApplicationFormDataErrors } from 'lib-common/application/validations'
import type { UpdateStateFn } from 'lib-common/form-state'
import { getErrorCount } from 'lib-common/form-validation'
import type {
  ApplicationDetails,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'

import EditorSection from '../EditorSection'
import type { ApplicationEditorDeps } from '../types'

import ChildSubSection from './ChildSubSection'
import GuardianSubSection from './GuardianSubSection'
import OtherChildrenSubSection from './OtherChildrenSubSection'
import OtherPartnerSubSection from './OtherPartnerSubSection'
import SecondGuardianSubSection from './SecondGuardianSubSection'

export type ContactInfoSectionProps = {
  deps: ApplicationEditorDeps
  type: ApplicationType
  application: ApplicationDetails
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
  const { translations: t } = props.deps

  return (
    <EditorSection
      deps={props.deps}
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
