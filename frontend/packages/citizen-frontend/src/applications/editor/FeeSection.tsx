// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { H2, P } from '@evaka/lib-components/src/typography'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { useTranslation } from '~localization'
import { FeeFormData } from '~applications/editor/ApplicationFormData'

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
    <ContentArea opaque paddingVertical="L">
      <H2>{t.applications.editor.fee.title}</H2>
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
    </ContentArea>
  )
})
