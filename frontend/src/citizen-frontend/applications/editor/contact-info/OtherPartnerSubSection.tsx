// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import type { ContactInfoSectionProps } from '../../../applications/editor/contact-info/ContactInfoSection'
import { errorToInputInfo } from '../../../input-info-helper'
import { useTranslation } from '../../../localization'

export default React.memo(function OtherPartnerSubSection({
  formData,
  updateFormData,
  errors,
  verificationRequested
}: ContactInfoSectionProps) {
  const t = useTranslation()

  return (
    <>
      <H3>{t.applications.editor.contactInfo.otherPartnerTitle}</H3>
      <Gap size="s" />
      <Checkbox
        label={t.applications.editor.contactInfo.otherPartnerCheckboxLabel}
        checked={formData.otherPartnerExists}
        data-qa="otherPartnerExists-input"
        onChange={(checked) => {
          updateFormData({
            otherPartnerExists: checked
          })
        }}
      />
      {formData.otherPartnerExists && (
        <>
          <Gap size="m" />
          <FixedSpaceRow spacing="XL">
            <AdaptiveFlex breakpoint="1060px">
              <FixedSpaceColumn spacing="xs">
                <Label htmlFor="other-partner-first-name">
                  {t.applications.editor.contactInfo.personFirstName + ' *'}
                </Label>
                <InputField
                  id="other-partner-first-name"
                  value={formData.otherPartnerFirstName}
                  data-qa="otherPartnerFirstName-input"
                  onChange={(value) =>
                    updateFormData({
                      otherPartnerFirstName: value
                    })
                  }
                  info={errorToInputInfo(
                    errors.otherPartnerFirstName,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.firstNamePlaceholder
                  }
                  width="L"
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label htmlFor="other-partner-last-name">
                  {t.applications.editor.contactInfo.personLastName + ' *'}
                </Label>
                <InputField
                  id="other-partner-last-name"
                  value={formData.otherPartnerLastName}
                  data-qa="otherPartnerLastName-input"
                  onChange={(value) =>
                    updateFormData({
                      otherPartnerLastName: value
                    })
                  }
                  info={errorToInputInfo(
                    errors.otherPartnerLastName,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.lastNamePlaceholder
                  }
                  width="m"
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label htmlFor="other-partner-ssn">
                  {t.applications.editor.contactInfo.personSSN + ' *'}
                </Label>
                <InputField
                  id="other-partner-ssn"
                  value={formData.otherPartnerSSN}
                  data-qa="otherPartnerSSN-input"
                  onChange={(value) =>
                    updateFormData({
                      otherPartnerSSN: value.toUpperCase()
                    })
                  }
                  info={errorToInputInfo(
                    errors.otherPartnerSSN,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={
                    !verificationRequested &&
                    formData.otherPartnerSSN.length < 11
                  }
                  placeholder={t.applications.editor.contactInfo.ssnPlaceholder}
                  width="m"
                />
              </FixedSpaceColumn>
            </AdaptiveFlex>
          </FixedSpaceRow>
        </>
      )}
    </>
  )
})
