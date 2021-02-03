// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { P } from '@evaka/lib-components/src/typography'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useTranslation } from '~localization'
import { FeeFormData } from '~applications/editor/ApplicationFormData'
import EditorSection from '~applications/editor/EditorSection'
import { ApplicationFormDataErrors } from '~applications/editor/validations'
import { getErrorCount } from '~form-validation'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'

type Props = {
  applicationType: ApplicationType
  formData: FeeFormData
  updateFormData: (v: Partial<FeeFormData>) => void
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
      <P
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.fee.info[applicationType]
        }}
      />
      <P
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.fee.emphasis
        }}
      />
      <Checkbox
        checked={formData.maxFeeAccepted}
        dataQa={'maxFeeAccepted-input'}
        label={t.applications.editor.fee.checkbox}
        onChange={(maxFeeAccepted) => updateFormData({ maxFeeAccepted })}
      />
      <P
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.fee.links
        }}
      />
    </EditorSection>
  )
})
