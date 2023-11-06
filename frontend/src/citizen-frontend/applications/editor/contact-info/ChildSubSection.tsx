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
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { errorToInputInfo } from '../../../input-info-helper'
import { useLang, useTranslation } from '../../../localization'

import { ContactInfoSectionProps } from './ContactInfoSection'

export default React.memo(function ChildSubSection({
  formData,
  updateFormData,
  errors,
  verificationRequested,
  fullFamily
}: ContactInfoSectionProps) {
  const t = useTranslation()
  const [lang] = useLang()

  return (
    <>
      <Gap size="m" />
      {fullFamily ? t.applications.editor.contactInfo.familyInfo : null}
      {t.applications.editor.contactInfo.info}
      <H3>{t.applications.editor.contactInfo.childInfoTitle}</H3>
      <Gap size="xs" />
      <FixedSpaceRow spacing="XL">
        <AdaptiveFlex breakpoint="1060px">
          <FixedSpaceColumn spacing="xs">
            <Label>{t.applications.editor.contactInfo.childFirstName}</Label>
            <span>{formData.childFirstName}</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <Label>{t.applications.editor.contactInfo.childLastName}</Label>
            <span>{formData.childLastName}</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <Label>{t.applications.editor.contactInfo.childSSN}</Label>
            <span>{formData.childSSN}</span>
          </FixedSpaceColumn>
        </AdaptiveFlex>
      </FixedSpaceRow>
      <Gap size="s" />
      <FixedSpaceColumn spacing="xs">
        <Label>{t.applications.editor.contactInfo.homeAddress}</Label>
        <span data-qa="child-street-address">{formData.childStreet}</span>
      </FixedSpaceColumn>
      <Gap size="m" />

      <ExpandingInfo
        data-qa="child-future-address-info"
        info={t.applications.editor.contactInfo.futureAddressInfo}
      >
        <Checkbox
          label={t.applications.editor.contactInfo.hasFutureAddress}
          checked={formData.childFutureAddressExists}
          data-qa="childFutureAddressExists-input"
          onChange={(checked) => {
            updateFormData({
              childFutureAddressExists: checked
            })
            if (!checked) {
              updateFormData({
                guardianFutureAddressEqualsChild: false
              })
            }
          }}
        />
      </ExpandingInfo>

      {formData.childFutureAddressExists && (
        <>
          <Gap size="m" />
          <FixedSpaceColumn spacing="xs">
            <Label>{t.applications.editor.contactInfo.moveDate + ' *'}</Label>
            <DatePicker
              date={formData.childMoveDate}
              data-qa="childMoveDate-input"
              onChange={(value) =>
                formData.guardianFutureAddressEqualsChild
                  ? updateFormData({
                      guardianMoveDate: value,
                      childMoveDate: value
                    })
                  : updateFormData({ childMoveDate: value })
              }
              locale={lang}
              info={errorToInputInfo(errors.childMoveDate, t.validationErrors)}
              hideErrorsBeforeTouched={!verificationRequested}
            />
          </FixedSpaceColumn>
          <Gap size="s" />
          <FixedSpaceRow spacing="XL">
            <AdaptiveFlex breakpoint="1060px">
              <FixedSpaceColumn spacing="xs">
                <Label htmlFor="child-future-street">
                  {t.applications.editor.contactInfo.street + ' *'}
                </Label>
                <InputField
                  id="child-future-street"
                  value={formData.childFutureStreet}
                  data-qa="childFutureStreet-input"
                  onChange={(value) =>
                    formData.guardianFutureAddressEqualsChild
                      ? updateFormData({
                          childFutureStreet: value,
                          guardianFutureStreet: value
                        })
                      : updateFormData({
                          childFutureStreet: value
                        })
                  }
                  info={errorToInputInfo(
                    errors.childFutureStreet,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.streetPlaceholder
                  }
                  width="L"
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label htmlFor="child-future-postal-code">
                  {t.applications.editor.contactInfo.postalCode + ' *'}
                </Label>
                <InputField
                  id="child-future-postal-code"
                  value={formData.childFuturePostalCode}
                  data-qa="childFuturePostalCode-input"
                  onChange={(value) =>
                    formData.guardianFutureAddressEqualsChild
                      ? updateFormData({
                          guardianFuturePostalCode: value,
                          childFuturePostalCode: value
                        })
                      : updateFormData({ childFuturePostalCode: value })
                  }
                  info={errorToInputInfo(
                    errors.childFuturePostalCode,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.postalCodePlaceholder
                  }
                  width="m"
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing="xs">
                <Label htmlFor="child-future-post-office">
                  {t.applications.editor.contactInfo.postOffice + ' *'}
                </Label>
                <InputField
                  id="child-future-post-office"
                  value={formData.childFuturePostOffice}
                  data-qa="childFuturePostOffice-input"
                  onChange={(value) =>
                    formData.guardianFutureAddressEqualsChild
                      ? updateFormData({
                          guardianFuturePostOffice: value,
                          childFuturePostOffice: value
                        })
                      : updateFormData({ childFuturePostOffice: value })
                  }
                  info={errorToInputInfo(
                    errors.childFuturePostOffice,
                    t.validationErrors
                  )}
                  hideErrorsBeforeTouched={!verificationRequested}
                  placeholder={
                    t.applications.editor.contactInfo.municipalityPlaceholder
                  }
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
