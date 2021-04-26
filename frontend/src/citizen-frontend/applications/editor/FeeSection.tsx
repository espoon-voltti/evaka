// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/api-types/application/enums'
import { UpdateStateFn } from 'lib-common/form-state'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import React from 'react'
import EditorSection from '../../applications/editor/EditorSection'
import { getErrorCount } from '../../form-validation'
import { useTranslation } from '../../localization'
import { FeeFormData } from './ApplicationFormData'
import { ApplicationFormDataErrors } from './validations'

type Props = {
  applicationType: ApplicationType
  formData: FeeFormData
  updateFormData: UpdateStateFn<FeeFormData>
  errors: ApplicationFormDataErrors['fee']
  verificationRequested: boolean
}

export default React.memo(function FeeSection({
  applicationType,
  formData,
  updateFormData,
  errors,
  verificationRequested
}: Props) {
  const t = useTranslation()

  return (
    <EditorSection
      title={t.applications.editor.fee.title}
      validationErrors={verificationRequested ? getErrorCount(errors) : 0}
      data-qa="fee-section"
    >
      {t.applications.editor.fee.info[applicationType]()}
      {t.applications.editor.fee.emphasis()}
      <Checkbox
        checked={formData.maxFeeAccepted}
        data-qa={'maxFeeAccepted-input'}
        label={t.applications.editor.fee.checkbox}
        onChange={(maxFeeAccepted) => updateFormData({ maxFeeAccepted })}
      />
      {t.applications.editor.fee.links()}
    </EditorSection>
  )
})
