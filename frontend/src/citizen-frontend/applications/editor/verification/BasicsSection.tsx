// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import React from 'react'
import { useTranslation } from '../../../localization'
import { H2, Label } from 'lib-components/typography'
import ListGrid from 'lib-components/layout/ListGrid'
import { ApplicationDataGridLabelWidth } from '../../../applications/editor/verification/const'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import { formatDate } from 'lib-common/date'

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
        <span>{formatDate(application.createdDate)}</span>

        {formatDate(application.modifiedDate) !==
          formatDate(application.createdDate) && (
          <>
            <Label>{t.applications.editor.verification.basics.modified}</Label>
            <span>{formatDate(application.modifiedDate)}</span>
          </>
        )}
      </ListGrid>
    </div>
  )
})
