// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { P } from '@evaka/lib-components/src/typography'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useTranslation } from '~localization'
import { FeeFormData } from '~applications/editor/ApplicationFormData'
import EditorSection from '~applications/editor/EditorSection'

type Props = {
  formData: FeeFormData
  updateFormData: (v: FeeFormData) => void
}

export default React.memo(function FeeSection({
  formData,
  updateFormData
}: Props) {
  const t = useTranslation()

  return (
    <EditorSection title={t.applications.editor.fee.title} validationErrors={0}>
      <P
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.fee.info
        }}
      />
      <P
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.fee.emphasis
        }}
      />
      <Checkbox
        checked={formData.maxFeeAccepted}
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
