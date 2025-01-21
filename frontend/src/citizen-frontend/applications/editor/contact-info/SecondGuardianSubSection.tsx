// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { ContactInfoFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  ApplicationType,
  OtherGuardianAgreementStatus
} from 'lib-common/generated/api-types/application'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { errorToInputInfo } from '../../../input-info-helper'
import { useTranslation } from '../../../localization'
import { ApplicationFormDataErrors } from '../validations'

type SecondGuardianSubSectionProps = {
  type: ApplicationType
  formData: ContactInfoFormData
  updateFormData: UpdateStateFn<ContactInfoFormData>
  errors: ApplicationFormDataErrors['contactInfo']
  verificationRequested: boolean
  otherGuardianStatus: 'NO' | 'SAME_ADDRESS' | 'DIFFERENT_ADDRESS'
}

export type SelectableOtherGuardianAgreementStatus = Exclude<
  OtherGuardianAgreementStatus,
  'AUTOMATED'
>

export default React.memo(function SecondGuardianSubSection({
  type,
  formData,
  updateFormData,
  errors,
  verificationRequested,
  otherGuardianStatus
}: SecondGuardianSubSectionProps) {
  const t = useTranslation()

  const agreementStatuses: SelectableOtherGuardianAgreementStatus[] = [
    'AGREED',
    'NOT_AGREED',
    'RIGHT_TO_GET_NOTIFIED'
  ]

  return (
    <>
      <H3>{t.applications.editor.contactInfo.secondGuardianInfoTitle}</H3>

      {(type === 'PRESCHOOL' || type === 'DAYCARE') && (
        <>
          {otherGuardianStatus === 'NO' && (
            <P>{t.applications.editor.contactInfo.secondGuardianNotFound}</P>
          )}
          {otherGuardianStatus === 'SAME_ADDRESS' && (
            <P>{t.applications.editor.contactInfo.secondGuardianInfo}</P>
          )}
          {otherGuardianStatus === 'DIFFERENT_ADDRESS' && (
            <>
              <P>
                {
                  t.applications.editor.contactInfo
                    .secondGuardianInfoPreschoolSeparated
                }
              </P>
              <FixedSpaceColumn>
                <Label>
                  {
                    t.applications.editor.contactInfo
                      .secondGuardianAgreementStatus.label
                  }{' '}
                  *
                </Label>
                {agreementStatuses.map((agreementStatus) => (
                  <Radio
                    key={agreementStatus}
                    checked={
                      formData.otherGuardianAgreementStatus === agreementStatus
                    }
                    data-qa={`otherGuardianAgreementStatus-${agreementStatus}`}
                    label={
                      t.applications.editor.contactInfo
                        .secondGuardianAgreementStatus[agreementStatus]
                    }
                    onChange={() =>
                      updateFormData({
                        otherGuardianAgreementStatus: agreementStatus
                      })
                    }
                  />
                ))}
              </FixedSpaceColumn>

              {verificationRequested && errors.otherGuardianAgreementStatus && (
                <>
                  <AlertBox
                    message={
                      t.validationErrors[errors.otherGuardianAgreementStatus]
                    }
                  />
                </>
              )}

              {formData.otherGuardianAgreementStatus === 'NOT_AGREED' && (
                <>
                  <Gap />
                  <AdaptiveFlex breakpoint="1060px">
                    <FixedSpaceColumn>
                      <Label htmlFor="other-guardian-phone">
                        {t.applications.editor.contactInfo.secondGuardianPhone}
                      </Label>
                      <InputField
                        id="other-guardian-phone"
                        value={formData.otherGuardianPhone}
                        data-qa="otherGuardianPhone-input"
                        onChange={(value) =>
                          updateFormData({
                            otherGuardianPhone: value
                          })
                        }
                        placeholder={
                          t.applications.editor.contactInfo.secondGuardianPhone
                        }
                        info={errorToInputInfo(
                          errors.otherGuardianPhone,
                          t.validationErrors
                        )}
                        hideErrorsBeforeTouched={!verificationRequested}
                        width="L"
                      />
                    </FixedSpaceColumn>
                    <FixedSpaceColumn>
                      <Label htmlFor="other-guardian-email">
                        {t.applications.editor.contactInfo.secondGuardianEmail}
                      </Label>
                      <InputField
                        id="other-guardian-email"
                        value={formData.otherGuardianEmail}
                        data-qa="otherGuardianEmail-input"
                        onChange={(value) =>
                          updateFormData({
                            otherGuardianEmail: value
                          })
                        }
                        placeholder={
                          t.applications.editor.contactInfo.secondGuardianEmail
                        }
                        info={errorToInputInfo(
                          errors.otherGuardianEmail,
                          t.validationErrors
                        )}
                        hideErrorsBeforeTouched={!verificationRequested}
                        width="L"
                      />
                    </FixedSpaceColumn>
                  </AdaptiveFlex>
                </>
              )}
            </>
          )}
        </>
      )}
    </>
  )
})
