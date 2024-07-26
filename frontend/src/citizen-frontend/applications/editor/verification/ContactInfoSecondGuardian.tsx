// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { ContactInfoFormData } from 'lib-common/api-types/application/ApplicationFormData'
import ListGrid from 'lib-components/layout/ListGrid'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../localization'

import { ApplicationDataGridLabelWidth } from './const'

type Props = {
  formData: ContactInfoFormData
}

export default React.memo(function ContactInfoSecondGuardian({
  formData
}: Props) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.contactInfo

  return (
    <>
      {formData.otherGuardianAgreementStatus === 'AGREED' && (
        <span>{tLocal.secondGuardian.agreed}</span>
      )}
      {formData.otherGuardianAgreementStatus === 'NOT_AGREED' && (
        <>
          <span>{tLocal.secondGuardian.notAgreed}</span>
          <ListGrid
            labelWidth={ApplicationDataGridLabelWidth}
            rowGap="s"
            columnGap="L"
          >
            <Label>{tLocal.secondGuardian.tel}</Label>
            <span>{formData.otherGuardianPhone}</span>

            <Label>{tLocal.secondGuardian.email}</Label>
            <span translate="no">{formData.otherGuardianEmail}</span>
          </ListGrid>
        </>
      )}
      {formData.otherGuardianAgreementStatus === 'RIGHT_TO_GET_NOTIFIED' && (
        <span>{tLocal.secondGuardian.rightToGetNotified}</span>
      )}
    </>
  )
})
