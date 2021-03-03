// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { H3, Label } from '@evaka/lib-components/typography'
import Checkbox from '@evaka/lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from '@evaka/lib-components/layout/flex-helpers'
import InputField from '@evaka/lib-components/atoms/form/InputField'
import { Gap } from '@evaka/lib-components/white-space'
import { useTranslation } from '../../../localization'
import { errorToInputInfo } from '../../../form-validation'
import AdaptiveFlex from '@evaka/lib-components/layout/AdaptiveFlex'
import { UnitPreferenceSectionCommonProps } from '../../../applications/editor/unit-preference/UnitPreferenceSection'
import Radio from '@evaka/lib-components/atoms/form/Radio'

export default React.memo(function SiblingBasisSubSection({
  applicationType,
  formData,
  updateFormData,
  errors,
  verificationRequested
}: UnitPreferenceSectionCommonProps) {
  const t = useTranslation()

  return (
    <>
      <H3>{t.applications.editor.unitPreference.siblingBasis.title}</H3>
      {t.applications.editor.unitPreference.siblingBasis.info[
        applicationType
      ]()}
      <Checkbox
        checked={formData.siblingBasis}
        dataQa="siblingBasis-input"
        label={
          t.applications.editor.unitPreference.siblingBasis.checkbox[
            applicationType
          ]
        }
        onChange={(checked) => updateFormData({ siblingBasis: checked })}
      />
      {formData.siblingBasis && (
        <>
          {formData.vtjSiblings.length > 0 && (
            <>
              <Gap />
              <Label>
                {
                  t.applications.editor.unitPreference.siblingBasis.radioLabel[
                    applicationType
                  ]
                }{' '}
                *
              </Label>
              <Gap />
              <FixedSpaceColumn>
                {formData.vtjSiblings.map((sibling) => (
                  <Radio
                    key={sibling.socialSecurityNumber}
                    checked={sibling.selected}
                    label={`${sibling.firstName} ${sibling.lastName}, ${sibling.socialSecurityNumber}`}
                    onChange={() =>
                      updateFormData({
                        vtjSiblings: formData.vtjSiblings.map((s) => ({
                          ...s,
                          selected:
                            s.socialSecurityNumber ===
                            sibling.socialSecurityNumber
                        }))
                      })
                    }
                  />
                ))}
                <Radio
                  checked={!formData.vtjSiblings.find((s) => s.selected)}
                  label={
                    t.applications.editor.unitPreference.siblingBasis
                      .otherSibling
                  }
                  dataQa={'other-sibling'}
                  onChange={() =>
                    updateFormData({
                      vtjSiblings: formData.vtjSiblings.map((s) => ({
                        ...s,
                        selected: false
                      }))
                    })
                  }
                />
              </FixedSpaceColumn>
            </>
          )}
          {!formData.vtjSiblings.find((s) => s.selected) && (
            <>
              <Gap size={'s'} />
              <AdaptiveFlex horizontalSpacing={'L'} breakpoint="1060px">
                <FixedSpaceColumn spacing={'xs'}>
                  <Label htmlFor={'sibling-names'}>
                    {t.applications.editor.unitPreference.siblingBasis.names} *
                  </Label>
                  <InputField
                    value={formData.siblingName}
                    dataQa={'siblingName-input'}
                    onChange={(value) => updateFormData({ siblingName: value })}
                    width={'XL'}
                    placeholder={
                      t.applications.editor.unitPreference.siblingBasis
                        .namesPlaceholder
                    }
                    id={'sibling-names'}
                    info={errorToInputInfo(
                      errors.siblingName,
                      t.validationErrors
                    )}
                    hideErrorsBeforeTouched={!verificationRequested}
                  />
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing={'xs'}>
                  <Label htmlFor={'sibling-ssn'}>
                    {t.applications.editor.unitPreference.siblingBasis.ssn} *
                  </Label>
                  <InputField
                    value={formData.siblingSsn}
                    dataQa={'siblingSsn-input'}
                    onChange={(value) =>
                      updateFormData({ siblingSsn: value.toUpperCase() })
                    }
                    placeholder={
                      t.applications.editor.unitPreference.siblingBasis
                        .ssnPlaceholder
                    }
                    id={'sibling-ssn'}
                    width="m"
                    info={errorToInputInfo(
                      errors.siblingSsn,
                      t.validationErrors
                    )}
                    hideErrorsBeforeTouched={
                      !verificationRequested && formData.siblingSsn.length < 11
                    }
                  />
                </FixedSpaceColumn>
              </AdaptiveFlex>
            </>
          )}
        </>
      )}
    </>
  )
})
