import React from 'react'
import { useTranslation } from '~localization'
import { ApplicationGuardianAgreementStatus } from '@evaka/lib-common/src/api-types/application/enums'

type Props = {
  agreementStatus: ApplicationGuardianAgreementStatus | null
}

export default React.memo(function ContactInfoSecondGuardianPreschool({
  agreementStatus
}: Props) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.contactInfo

  return (
    <>
      {agreementStatus === 'AGREED' && (
        <span>{tLocal.secondGuardian.agreed}</span>
      )}
      {(agreementStatus === 'NOT_AGREED' ||
        agreementStatus === 'RIGHT_TO_GET_NOTIFIED') && (
        <span>{tLocal.secondGuardian.notAgreed}</span>
      )}
      {!agreementStatus && (
        <span>{tLocal.secondGuardian.noAgreementStatus}</span>
      )}
    </>
  )
})
