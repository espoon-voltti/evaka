import { ApplicationFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { H2, Label } from '@evaka/lib-components/src/typography'
import { Application } from '~applications/types'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { DATE_FORMAT_DATE_DEFAULT, formatDate } from '~util'
import { DaycareApplicationVerificationLabelWidth } from '~applications/editor/verification/DaycareApplicationVerificationView'

type BasicsSectionProps = {
  application: Application
  formData: ApplicationFormData
}

export default React.memo(function BasicsSection({
  application,
  formData
}: BasicsSectionProps) {
  const t = useTranslation()
  return (
    <div>
      <H2>
        {formData.contactInfo.childFirstName}{' '}
        {formData.contactInfo.childLastName}
      </H2>

      <ListGrid
        labelWidth={DaycareApplicationVerificationLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{t.applications.editor.verification.basics.created}</Label>
        <span>
          {formatDate(application.createdDate, DATE_FORMAT_DATE_DEFAULT)}
        </span>

        <Label>{t.applications.editor.verification.basics.modified}</Label>
        <span>
          {formatDate(application.modifiedDate, DATE_FORMAT_DATE_DEFAULT)}
        </span>
      </ListGrid>
    </div>
  )
})
