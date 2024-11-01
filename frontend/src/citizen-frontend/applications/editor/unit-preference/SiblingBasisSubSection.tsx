// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { errorToInputInfo } from '../../../input-info-helper'
import { useTranslation } from '../../../localization'

import { UnitPreferenceSectionCommonProps } from './UnitPreferenceSection'

export default React.memo(function SiblingBasisSubSection({
  applicationType,
  formData,
  updateFormData,
  errors,
  verificationRequested
}: UnitPreferenceSectionCommonProps) {
  const t = useTranslation()

  const siblingUnitEnabled = applicationType === 'PRESCHOOL'
  const siblingUnitEditor = (
    <>
      <Label htmlFor="sibling-unit">
        {t.applications.editor.unitPreference.siblingBasis.unit} *
      </Label>
      <InputField
        value={formData.siblingUnit}
        data-qa="siblingUnit-input"
        onChange={(value) =>
          updateFormData(() => ({
            siblingUnit: value
          }))
        }
        placeholder={
          t.applications.editor.unitPreference.siblingBasis.unitPlaceholder
        }
        id="sibling-unit"
        width="m"
        info={errorToInputInfo(errors.siblingUnit, t.validationErrors)}
        hideErrorsBeforeTouched={!verificationRequested}
      />
    </>
  )

  return (
    <>
      <H3>{t.applications.editor.unitPreference.siblingBasis.title}</H3>
      {t.applications.editor.unitPreference.siblingBasis.info[applicationType]}
      <Checkbox
        checked={formData.siblingBasis}
        data-qa="siblingBasis-input"
        label={
          t.applications.editor.unitPreference.siblingBasis.checkbox[
            applicationType
          ]
        }
        onChange={(checked) =>
          updateFormData(() => ({ siblingBasis: checked }))
        }
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
                  <AdaptiveFlex key={sibling.socialSecurityNumber}>
                    <Radio
                      checked={sibling.selected}
                      label={`${sibling.firstName} ${sibling.lastName}, ${sibling.socialSecurityNumber}`}
                      translate="no"
                      onChange={() =>
                        updateFormData((prev) => ({
                          vtjSiblings: prev.vtjSiblings.map((s) => ({
                            ...s,
                            selected:
                              s.socialSecurityNumber ===
                              sibling.socialSecurityNumber
                          }))
                        }))
                      }
                    />
                    {sibling.selected &&
                      siblingUnitEnabled &&
                      siblingUnitEditor}
                  </AdaptiveFlex>
                ))}
                <Radio
                  checked={!formData.vtjSiblings.find((s) => s.selected)}
                  label={
                    t.applications.editor.unitPreference.siblingBasis
                      .otherSibling
                  }
                  data-qa="other-sibling"
                  onChange={() =>
                    updateFormData((prev) => ({
                      vtjSiblings: prev.vtjSiblings.map((s) => ({
                        ...s,
                        selected: false
                      }))
                    }))
                  }
                />
              </FixedSpaceColumn>
            </>
          )}
          {!formData.vtjSiblings.find((s) => s.selected) && (
            <>
              <Gap size="s" />
              <AdaptiveFlex horizontalSpacing="L" breakpoint="1060px">
                <FixedSpaceColumn spacing="xs">
                  <Label htmlFor="sibling-names">
                    {t.applications.editor.unitPreference.siblingBasis.names} *
                  </Label>
                  <InputField
                    value={formData.siblingName}
                    data-qa="siblingName-input"
                    onChange={(value) =>
                      updateFormData(() => ({ siblingName: value }))
                    }
                    width="XL"
                    placeholder={
                      t.applications.editor.unitPreference.siblingBasis
                        .namesPlaceholder
                    }
                    id="sibling-names"
                    info={errorToInputInfo(
                      errors.siblingName,
                      t.validationErrors
                    )}
                    hideErrorsBeforeTouched={!verificationRequested}
                  />
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xs">
                  <Label htmlFor="sibling-ssn">
                    {t.applications.editor.unitPreference.siblingBasis.ssn} *
                  </Label>
                  <InputField
                    value={formData.siblingSsn}
                    data-qa="siblingSsn-input"
                    onChange={(value) =>
                      updateFormData(() => ({
                        siblingSsn: value.toUpperCase()
                      }))
                    }
                    placeholder={
                      t.applications.editor.unitPreference.siblingBasis
                        .ssnPlaceholder
                    }
                    id="sibling-ssn"
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
                {siblingUnitEnabled && (
                  <FixedSpaceColumn spacing="xs">
                    {siblingUnitEditor}
                  </FixedSpaceColumn>
                )}
              </AdaptiveFlex>
            </>
          )}
        </>
      )}
    </>
  )
})
