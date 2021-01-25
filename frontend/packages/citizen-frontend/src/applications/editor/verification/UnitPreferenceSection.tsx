import { UnitPreferenceFormData } from '~applications/editor/ApplicationFormData'
import React from 'react'
import { useTranslation } from '~localization'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { Gap } from '@evaka/lib-components/src/white-space'
import styled from 'styled-components'
import { DaycareApplicationVerificationLabelWidth } from '~applications/editor/verification/DaycareApplicationVerificationView'

const NumberedList = styled.ol`
  margin: 0;
  padding-left: 16px;
`

type UnitPreferenceSectionProps = {
  formData: UnitPreferenceFormData
}

export default React.memo(function UnitPreferenceSection({
  formData
}: UnitPreferenceSectionProps) {
  const t = useTranslation()
  const tLocal = t.applications.editor.verification.unitPreference

  return (
    <div>
      <H2 noMargin>{tLocal.title}</H2>

      <Gap size="s" />

      <H3>{tLocal.siblingBasis.title}</H3>
      <ListGrid
        labelWidth={DaycareApplicationVerificationLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{tLocal.siblingBasis.siblingBasisLabel}</Label>
        {formData.siblingBasis ? (
          <>
            <span>{tLocal.siblingBasis.siblingBasisYes}</span>

            <Label>{tLocal.siblingBasis.name}</Label>
            <span>{formData.siblingName}</span>

            <Label>{tLocal.siblingBasis.ssn}</Label>
            <span>{formData.siblingSsn}</span>
          </>
        ) : (
          <span>{t.applications.editor.verification.no}</span>
        )}
      </ListGrid>

      <Gap size="m" />

      <H3>{tLocal.units.title}</H3>
      <ListGrid
        labelWidth={DaycareApplicationVerificationLabelWidth}
        rowGap="s"
        columnGap="L"
      >
        <Label>{tLocal.units.label}</Label>
        <NumberedList>
          {formData.preferredUnits.map((u) => (
            <li key={u.id}>{u.name}</li>
          ))}
        </NumberedList>
      </ListGrid>
    </div>
  )
})
