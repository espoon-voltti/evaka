import React from 'react'
import { useTranslation } from '~localization'
import { H3 } from '@evaka/lib-components/src/typography'
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
      <H3>{tLocal.secondGuardian.title}</H3>
      {agreementStatus &&
        (agreementStatus === 'AGREED' ? (
          <span>{tLocal.secondGuardian.agreed}</span>
        ) : (
          <span>{tLocal.secondGuardian.notAgreed}</span>
        ))}
    </>
  )
})
