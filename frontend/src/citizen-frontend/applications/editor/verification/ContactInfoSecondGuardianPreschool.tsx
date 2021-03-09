import React from 'react'
import { useTranslation } from '../../../localization'
import { ContactInfoFormData } from '../ApplicationFormData'
import { ApplicationDataGridLabelWidth } from './const'
import ListGrid from '@evaka/lib-components/layout/ListGrid'
import { Label } from '@evaka/lib-components/typography'

type Props = {
  formData: ContactInfoFormData
}

export default React.memo(function ContactInfoSecondGuardianPreschool({
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
            <span>{formData.otherGuardianEmail}</span>
          </ListGrid>
        </>
      )}
      {formData.otherGuardianAgreementStatus === 'RIGHT_TO_GET_NOTIFIED' && (
        <span>{tLocal.secondGuardian.rightToGetNotified}</span>
      )}
    </>
  )
})
