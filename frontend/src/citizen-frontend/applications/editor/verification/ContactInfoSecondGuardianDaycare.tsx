import React from 'react'
import { useTranslation } from '../../../localization'

export default React.memo(function ContactInfoSecondGuardianDaycare() {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.contactInfo

  return <span>{tLocal.secondGuardian.info}</span>
})
