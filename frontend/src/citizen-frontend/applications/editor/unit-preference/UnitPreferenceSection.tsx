// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/api-types/application/enums'
import { UpdateStateFn } from 'lib-common/form-state'
import LocalDate from 'lib-common/local-date'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import React from 'react'
import EditorSection from '../../../applications/editor/EditorSection'
import SiblingBasisSubSection from '../../../applications/editor/unit-preference/SiblingBasisSubSection'
import UnitsSubSection from '../../../applications/editor/unit-preference/UnitsSubSection'
import { getErrorCount } from '../../../form-validation'
import { useTranslation } from '../../../localization'
import { UnitPreferenceFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { ApplicationFormDataErrors } from '../validations'

export type UnitPreferenceSectionCommonProps = {
  formData: UnitPreferenceFormData
  updateFormData: UpdateStateFn<UnitPreferenceFormData>
  errors: ApplicationFormDataErrors['unitPreference']
  verificationRequested: boolean
  applicationType: ApplicationType
  preparatory: boolean
  preferredStartDate: LocalDate | null
  shiftCare: boolean
}

export type UnitPreferenceSectionProps = UnitPreferenceSectionCommonProps & {
  applicationType: ApplicationType
  preparatory: boolean
  preferredStartDate: LocalDate | null
  shiftCare: boolean
}

export default React.memo(function UnitPreferenceSection(
  props: UnitPreferenceSectionProps
) {
  const t = useTranslation()

  return (
    <EditorSection
      title={t.applications.editor.unitPreference.title}
      validationErrors={
        props.verificationRequested ? getErrorCount(props.errors) : 0
      }
      data-qa="unitPreference-section"
    >
      <SiblingBasisSubSection {...props} />
      <HorizontalLine />
      <UnitsSubSection {...props} />
    </EditorSection>
  )
})
