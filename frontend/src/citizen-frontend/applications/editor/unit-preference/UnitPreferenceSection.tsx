// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { UnitPreferenceFormData } from '../../../applications/editor/ApplicationFormData'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '../../../localization'
import EditorSection from '../../../applications/editor/EditorSection'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'
import { ApplicationFormDataErrors } from '../../../applications/editor/validations'
import { getErrorCount } from '../../../form-validation'
import SiblingBasisSubSection from '../../../applications/editor/unit-preference/SiblingBasisSubSection'
import UnitsSubSection from '../../../applications/editor/unit-preference/UnitsSubSection'

export type UnitPreferenceSectionCommonProps = {
  formData: UnitPreferenceFormData
  updateFormData: (update: Partial<UnitPreferenceFormData>) => void
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
