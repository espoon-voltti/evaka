// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useLang, useTranslation } from '~localization'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { H3, Label } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { Gap } from '@evaka/lib-components/src/white-space'
import { errorToInputInfo } from '~form-validation'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import DatePicker from '@evaka/lib-components/src/molecules/date-picker/DatePicker'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import { ContactInfoSectionProps } from '~applications/editor/contact-info/ContactInfoSection'
import ExpandingInfo from '@evaka/lib-components/src/molecules/ExpandingInfo'

export default React.memo(function GuardianSubSection({
  formData,
  updateFormData,
  errors,
  verificationRequested
}: ContactInfoSectionProps) {
  const t = useTranslation()
  const [lang] = useLang()

  return (
    <>
      <H3>{t.applications.editor.contactInfo.guardianInfoTitle}</H3>

      <AdaptiveFlex breakpoint="1060px" horizontalSpacing="XL">
        <FixedSpaceColumn spacing={'xs'}>
          <Label>{t.applications.editor.contactInfo.guardianFirstName}</Label>
          <span>{formData.guardianFirstName}</span>
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing={'xs'}>
          <Label>{t.applications.editor.contactInfo.guardianLastName}</Label>
          <span>{formData.guardianLastName}</span>
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing={'xs'}>
          <Label>{t.applications.editor.contactInfo.guardianSSN}</Label>
          <span>{formData.guardianSSN}</span>
        </FixedSpaceColumn>
      </AdaptiveFlex>

      <Gap size={'s'} />

      <FixedSpaceColumn spacing={'xs'}>
        <Label>{t.applications.editor.contactInfo.homeAddress}</Label>
        <span>{formData.guardianHomeAddress}</span>
      </FixedSpaceColumn>

      <Gap size={'s'} />

      <FixedSpaceRow spacing={'XL'}>
        <AdaptiveFlex breakpoint="860px">
          <FixedSpaceColumn spacing={'xs'}>
            <Label htmlFor={'guardian-phone'}>
              {t.applications.editor.contactInfo.phone + ' *'}
            </Label>
            <InputField
              id={'guardian-phone'}
              value={formData.guardianPhone}
              dataQa={'guardianPhone-input'}
              onChange={(value) => updateFormData({ guardianPhone: value })}
              info={errorToInputInfo(errors.guardianPhone, t.validationErrors)}
              hideErrorsBeforeTouched={!verificationRequested}
              placeholder={t.applications.editor.contactInfo.phone}
              width="m"
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xs'}>
            <Label htmlFor={'guardian-email'}>
              {t.applications.editor.contactInfo.emailAddress}
            </Label>
            <InputField
              id={'guardian-email'}
              value={formData.guardianEmail}
              dataQa={'guardianEmail-input'}
              onChange={(value) => updateFormData({ guardianEmail: value })}
              info={errorToInputInfo(errors.guardianEmail, t.validationErrors)}
              hideErrorsBeforeTouched={!verificationRequested}
              placeholder={t.applications.editor.contactInfo.email}
              width={'L'}
            />
          </FixedSpaceColumn>
        </AdaptiveFlex>
      </FixedSpaceRow>
      <Gap size={'m'} />

      <ExpandingInfo info={t.applications.editor.contactInfo.futureAddressInfo}>
        <Checkbox
          label={t.applications.editor.contactInfo.hasFutureAddress}
          checked={formData.guardianFutureAddressExists}
          dataQa={'guardianFutureAddressExists-input'}
          onChange={(checked) => {
            updateFormData({
              guardianFutureAddressExists: checked
            })
          }}
        />
      </ExpandingInfo>
      {formData.guardianFutureAddressExists && (
        <>
          <Gap size={'m'} />
          {formData.childFutureAddressExists && (
            <>
              <Checkbox
                label={
                  t.applications.editor.contactInfo
                    .guardianFutureAddressEqualsChildFutureAddress
                }
                checked={formData.guardianFutureAddressEqualsChild}
                dataQa={'guardianFutureAddressEqualsChild-input'}
                onChange={(checked) => {
                  updateFormData({
                    guardianFutureAddressEqualsChild: checked
                  })
                  if (checked) {
                    updateFormData({
                      guardianMoveDate: formData.childMoveDate,
                      guardianFutureStreet: formData.childFutureStreet,
                      guardianFuturePostalCode: formData.childFuturePostalCode,
                      guardianFuturePostOffice: formData.childFuturePostOffice
                    })
                  }
                }}
              />
              <Gap size={'m'} />
            </>
          )}
          <FixedSpaceColumn spacing={'xs'}>
            <Label>{t.applications.editor.contactInfo.moveDate + ' *'}</Label>
            <DatePicker
              date={formData.guardianMoveDate}
              data-qa={'guardianMoveDate-input'}
              onChange={(value) => updateFormData({ guardianMoveDate: value })}
              locale={lang}
              info={errorToInputInfo(
                errors.guardianMoveDate,
                t.validationErrors
              )}
              hideErrorsBeforeTouched={!verificationRequested}
            />
            {verificationRequested && errors.guardianMoveDate && (
              <AlertBox
                thin
                message={t.validationErrors[errors.guardianMoveDate]}
              />
            )}
          </FixedSpaceColumn>
          <Gap size={'s'} />
          <FixedSpaceRow spacing={'XL'}>
            <AdaptiveFlex breakpoint="1060px">
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'guardian-future-street'}>
                  {t.applications.editor.contactInfo.street + ' *'}
                </Label>
                <InputField
                  id={'guardian-future-street'}
                  value={formData.guardianFutureStreet}
                  dataQa={'guardianFutureStreet-input'}
                  onChange={(value) =>
                    updateFormData({
                      guardianFutureStreet: value
                    })
                  }
                  info={errorToInputInfo(
                    errors.guardianFutureStreet,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.streetPlaceholder
                  }
                  readonly={formData.guardianFutureAddressEqualsChild}
                  width={'L'}
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'guardian-future-postal-code'}>
                  {t.applications.editor.contactInfo.postalCode + ' *'}
                </Label>
                <InputField
                  id={'guardian-future-postal-code'}
                  value={formData.guardianFuturePostalCode}
                  dataQa={'guardianFuturePostalCode-input'}
                  onChange={(value) =>
                    updateFormData({
                      guardianFuturePostalCode: value
                    })
                  }
                  info={errorToInputInfo(
                    errors.guardianFuturePostalCode,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.postalCodePlaceholder
                  }
                  readonly={formData.guardianFutureAddressEqualsChild}
                  width="m"
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'guardian-future-post-office'}>
                  {t.applications.editor.contactInfo.postOffice + ' *'}
                </Label>
                <InputField
                  id={'guardian-future-post-office'}
                  value={formData.guardianFuturePostOffice}
                  dataQa={'guardianFuturePostOffice-input'}
                  onChange={(value) =>
                    updateFormData({
                      guardianFuturePostOffice: value
                    })
                  }
                  info={errorToInputInfo(
                    errors.guardianFuturePostOffice,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.municipalityPlaceholder
                  }
                  readonly={formData.guardianFutureAddressEqualsChild}
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
