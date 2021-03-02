// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '../../../applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '../../../localization'
import { H2, Label } from '@evaka/lib-components/src/typography'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { DATE_FORMAT_DATE_DEFAULT, formatDate } from '../../../util'
import { ApplicationDataGridLabelWidth } from '../../../applications/editor/verification/const'
import { ApplicationDetails } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'

type BasicsSectionProps = {
  application: ApplicationDetails
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
        labelWidth={ApplicationDataGridLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{t.applications.editor.verification.basics.created}</Label>
        <span>
          {formatDate(application.createdDate, DATE_FORMAT_DATE_DEFAULT)}
        </span>

        {formatDate(application.modifiedDate, DATE_FORMAT_DATE_DEFAULT) !==
          formatDate(application.createdDate, DATE_FORMAT_DATE_DEFAULT) && (
          <>
            <Label>{t.applications.editor.verification.basics.modified}</Label>
            <span>
              {formatDate(application.modifiedDate, DATE_FORMAT_DATE_DEFAULT)}
            </span>
          </>
        )}
      </ListGrid>
    </div>
  )
})
