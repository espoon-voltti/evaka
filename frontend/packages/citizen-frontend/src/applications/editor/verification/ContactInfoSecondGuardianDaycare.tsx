import React from 'react'
import { useTranslation } from '~localization'
import { H3 } from '@evaka/lib-components/src/typography'

export default React.memo(function ContactInfoSecondGuardianDaycare() {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.contactInfo

  return (
    <>
      <H3>{tLocal.secondGuardian.title}</H3>
      <span>{tLocal.secondGuardian.info}</span>
    </>
  )
})
