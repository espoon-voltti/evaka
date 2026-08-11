// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type {
  ContactInfoFormData,
  SelectableOtherGuardianAgreementStatus
} from 'lib-common/application/ApplicationFormData'
import type { ApplicationFormDataErrors } from 'lib-common/application/validations'
import type { UpdateStateFn } from 'lib-common/form-state'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { errorToInputInfo } from 'lib-components/input-info-helper'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import type { ApplicationEditorDeps } from '../types'

type OtherGuardianContactFieldsProps = {
  translations: ApplicationEditorDeps['translations']
  formData: ContactInfoFormData
  updateFormData: UpdateStateFn<ContactInfoFormData>
  errors: ApplicationFormDataErrors['contactInfo']
  verificationRequested: boolean
  phoneDataQa: string
  emailDataQa: string
  withPlaceholders: boolean
}

const OtherGuardianContactFields = React.memo(
  function OtherGuardianContactFields({
    translations: t,
    formData,
    updateFormData,
    errors,
    verificationRequested,
    phoneDataQa,
    emailDataQa,
    withPlaceholders
  }: OtherGuardianContactFieldsProps) {
    const phoneLabel = t.applications.editor.contactInfo.secondGuardianPhone
    const emailLabel = t.applications.editor.contactInfo.secondGuardianEmail

    return (
      <AdaptiveFlex $breakpoint="1060px">
        <FixedSpaceColumn>
          <Label htmlFor="other-guardian-phone">{phoneLabel}</Label>
          <InputField
            id="other-guardian-phone"
            type="tel"
            value={formData.otherGuardianPhone}
            data-qa={phoneDataQa}
            onChange={(value) => updateFormData({ otherGuardianPhone: value })}
            placeholder={withPlaceholders ? phoneLabel : undefined}
            info={errorToInputInfo(
              errors.otherGuardianPhone,
              t.validationErrors
            )}
            hideErrorsBeforeTouched={!verificationRequested}
            width="L"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label htmlFor="other-guardian-email">{emailLabel}</Label>
          <InputField
            id="other-guardian-email"
            type="email"
            value={formData.otherGuardianEmail}
            data-qa={emailDataQa}
            onChange={(value) => updateFormData({ otherGuardianEmail: value })}
            placeholder={withPlaceholders ? emailLabel : undefined}
            info={errorToInputInfo(
              errors.otherGuardianEmail,
              t.validationErrors
            )}
            hideErrorsBeforeTouched={!verificationRequested}
            width="L"
          />
        </FixedSpaceColumn>
      </AdaptiveFlex>
    )
  }
)

type SecondGuardianSubSectionProps = {
  deps: ApplicationEditorDeps
  type: ApplicationType
  formData: ContactInfoFormData
  updateFormData: UpdateStateFn<ContactInfoFormData>
  errors: ApplicationFormDataErrors['contactInfo']
  verificationRequested: boolean
  otherGuardianStatus: 'NO' | 'SAME_ADDRESS' | 'DIFFERENT_ADDRESS'
}

export default React.memo(function SecondGuardianSubSection({
  deps,
  type,
  formData,
  updateFormData,
  errors,
  verificationRequested,
  otherGuardianStatus
}: SecondGuardianSubSectionProps) {
  const { translations: t, employeeTexts } = deps

  const agreementStatuses: SelectableOtherGuardianAgreementStatus[] = [
    'AGREED',
    'NOT_AGREED',
    'RIGHT_TO_GET_NOTIFIED'
  ]

  if (employeeTexts) {
    // Employees can also record that the status was never set, which citizens
    // cannot choose for themselves.
    const employeeAgreementStatuses: (SelectableOtherGuardianAgreementStatus | null)[] =
      [...agreementStatuses, null]
    return (
      <>
        <H3>{t.applications.editor.contactInfo.secondGuardianInfoTitle}</H3>
        <Checkbox
          checked={formData.otherGuardianExists}
          label={employeeTexts.secondGuardianExists}
          data-qa="application-second-guardian-toggle"
          onChange={(checked) => {
            updateFormData({ otherGuardianExists: checked })
            if (!checked) {
              updateFormData({
                otherGuardianAgreementStatus: null,
                otherGuardianPhone: '',
                otherGuardianEmail: ''
              })
            }
          }}
        />
        {formData.otherGuardianExists && (
          <>
            <Gap $size="s" />
            <OtherGuardianContactFields
              translations={t}
              formData={formData}
              updateFormData={updateFormData}
              errors={errors}
              verificationRequested={verificationRequested}
              phoneDataQa="application-second-guardian-phone"
              emailDataQa="application-second-guardian-email"
              withPlaceholders={false}
            />
            <Gap $size="s" />
            <FixedSpaceColumn>
              <Label>
                {
                  t.applications.editor.contactInfo
                    .secondGuardianAgreementStatus.label
                }
              </Label>
              {employeeAgreementStatuses.map((status) => (
                <Radio
                  key={status ?? 'NOT_SET'}
                  checked={formData.otherGuardianAgreementStatus === status}
                  data-qa={`radio-other-guardian-agreement-status-${status ?? 'null'}`}
                  label={
                    status !== null
                      ? t.applications.editor.contactInfo
                          .secondGuardianAgreementStatus[status]
                      : employeeTexts.secondGuardianAgreementStatusNotSet
                  }
                  onChange={() =>
                    updateFormData({ otherGuardianAgreementStatus: status })
                  }
                />
              ))}
            </FixedSpaceColumn>
          </>
        )}
      </>
    )
  }

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
              <FixedSpaceColumn aria-labelledby="second-guardian-agreement-label">
                <Label id="second-guardian-agreement-label">
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
                  <OtherGuardianContactFields
                    translations={t}
                    formData={formData}
                    updateFormData={updateFormData}
                    errors={errors}
                    verificationRequested={verificationRequested}
                    phoneDataQa="otherGuardianPhone-input"
                    emailDataQa="otherGuardianEmail-input"
                    withPlaceholders={true}
                  />
                </>
              )}
            </>
          )}
        </>
      )}
    </>
  )
})
