// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { useTranslation } from '~localization'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { H2, H3, Label, P } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { ContactInfoFormData } from './ApplicationFormData'
import { DatePickerClearable } from '@evaka/lib-components/src/molecules/DatePicker'

export type ContactInfoProps = {
  formData: ContactInfoFormData
  updateFormData: (update: Partial<ContactInfoFormData>) => void
}

export default React.memo(function ContactInfoSection({
  formData,
  updateFormData
}: ContactInfoProps) {
  const t = useTranslation()
  const [
    childAddressAboutToChangeChecked,
    setChildAddressAboutToChangeChecked
  ] = useState<boolean>(false)
  const [
    caretakerAddressAboutToChangeChecked,
    setCaretakerAddressAboutToChangeChecked
  ] = useState<boolean>(false)
  const [
    nonCaretakerCheckboxChecked,
    setNonCaretakerCheckboxChecked
  ] = useState<boolean>(false)

  return (
    <ContentArea opaque paddingVertical="L">
      <H2>{t.applications.editor.contactInfo.title}</H2>
      <p>{t.applications.editor.contactInfo.info}</p>
      <H3>{t.applications.editor.contactInfo.childInfoTitle}</H3>
      <FixedSpaceRow>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.childFirstName}</Label>
          <P>{formData.childFirstName}</P>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.childLastName}</Label>
          <P>{formData.childLastName}</P>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.childSSN}</Label>
          <P>{formData.childSSN}</P>
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <FixedSpaceRow>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.homeAddress}</Label>
          <P>{formData.childStreet}</P>
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <Checkbox
        label={t.applications.editor.contactInfo.hasFutureAddress}
        checked={childAddressAboutToChangeChecked}
        onChange={() => {
          setChildAddressAboutToChangeChecked(!childAddressAboutToChangeChecked)
          updateFormData({
            childHasFutureAddress: childAddressAboutToChangeChecked
          })
        }}
      />
      {childAddressAboutToChangeChecked && (
        <>
          <FixedSpaceRow>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.moveDate}</Label>
              <DatePickerClearable
                date={formData.childMoveDate}
                onChange={(value) => updateFormData({ childMoveDate: value })}
                onCleared={() => updateFormData({ childMoveDate: undefined })}
                placeholder={
                  t.applications.editor.contactInfo.choosePlaceholder
                }
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceRow>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.street}</Label>
              <InputField
                value={formData.childFutureAddress || ''}
                onChange={(value) =>
                  updateFormData({
                    childFutureAddress: value.length > 0 ? value : undefined
                  })
                }
                placeholder={
                  t.applications.editor.contactInfo.streetPlaceholder
                }
              />
            </FixedSpaceColumn>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.postalCode}</Label>
              <InputField
                value={formData.childFuturePostalCode || ''}
                onChange={(value) =>
                  updateFormData({
                    childFuturePostalCode: value.length > 0 ? value : undefined
                  })
                }
                placeholder={
                  t.applications.editor.contactInfo.postalCodePlaceholder
                }
              />
            </FixedSpaceColumn>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.municipality}</Label>
              <InputField
                value={formData.childFutureMunicipality || ''}
                onChange={(value) =>
                  updateFormData({
                    childFutureMunicipality:
                      value.length > 0 ? value : undefined
                  })
                }
                placeholder={
                  t.applications.editor.contactInfo.municipalityPlaceholder
                }
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </>
      )}
      <hr />
      <H3>{t.applications.editor.contactInfo.guardianInfoTitle}</H3>
      <FixedSpaceRow>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.guardianFirstName}</Label>
          <P>{formData.guardianFirstName}</P>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.guardianLastName}</Label>
          <P>{formData.guardianLastName}</P>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.guardianSSN}</Label>
          <P>{formData.guardianSSN}</P>
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <FixedSpaceRow>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.homeAddress}</Label>
          <P>{formData.guardianHomeAddress}</P>
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <FixedSpaceRow>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.phone}</Label>
          <InputField
            value={formData.guardianPhone}
            onChange={(value) => updateFormData({ guardianPhone: value })}
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{t.applications.editor.contactInfo.email}</Label>
          <InputField
            value={formData.guardianEmail}
            onChange={(value) => updateFormData({ guardianEmail: value })}
          />
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <Checkbox
        label={t.applications.editor.contactInfo.hasFutureAddress}
        checked={caretakerAddressAboutToChangeChecked}
        onChange={() => {
          setCaretakerAddressAboutToChangeChecked(
            !caretakerAddressAboutToChangeChecked
          )
          updateFormData({
            guardianHasFutureAddress: caretakerAddressAboutToChangeChecked
          })
        }}
      />
      {caretakerAddressAboutToChangeChecked && (
        <>
          <FixedSpaceRow>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.moveDate}</Label>
              <DatePickerClearable
                date={formData.guardianMoveDate}
                onChange={(value) =>
                  updateFormData({ guardianMoveDate: value })
                }
                onCleared={() =>
                  updateFormData({ guardianMoveDate: undefined })
                }
                placeholder={
                  t.applications.editor.contactInfo.choosePlaceholder
                }
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceRow>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.street}</Label>
              <InputField
                value={formData.guardianFutureStreet || ''}
                onChange={(value) =>
                  updateFormData({
                    guardianFutureStreet: value.length > 0 ? value : undefined
                  })
                }
                placeholder={
                  t.applications.editor.contactInfo.streetPlaceholder
                }
              />
            </FixedSpaceColumn>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.postalCode}</Label>
              <InputField
                value={formData.guardianFuturePostalCode || ''}
                onChange={(value) =>
                  updateFormData({
                    guardianFuturePostalCode:
                      value.length > 0 ? value : undefined
                  })
                }
                placeholder={
                  t.applications.editor.contactInfo.postalCodePlaceholder
                }
              />
            </FixedSpaceColumn>
            <FixedSpaceColumn>
              <Label>{t.applications.editor.contactInfo.municipality}</Label>
              <InputField
                value={formData.guardianFutureMunicipality || ''}
                onChange={(value) =>
                  updateFormData({
                    guardianFutureMunicipality:
                      value.length > 0 ? value : undefined
                  })
                }
                placeholder={
                  t.applications.editor.contactInfo.municipalityPlaceholder
                }
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </>
      )}
      <hr />
      <H3>{t.applications.editor.contactInfo.secondGuardianInfoTitle}</H3>
      <FixedSpaceColumn>
        <Label>{t.applications.editor.contactInfo.secondGuardianInfo}</Label>
      </FixedSpaceColumn>
      <hr />
      <H3>{t.applications.editor.contactInfo.nonCaretakerPartnerTitle}</H3>
      <Checkbox
        label={
          t.applications.editor.contactInfo.nonCaretakerPartnerCheckboxLabel
        }
        checked={nonCaretakerCheckboxChecked}
        onChange={() => {
          setNonCaretakerCheckboxChecked(!nonCaretakerCheckboxChecked)
          updateFormData({
            nonCaretakerPartnerInSameAddress: nonCaretakerCheckboxChecked
          })
        }}
      />
      {nonCaretakerCheckboxChecked && (
        <FixedSpaceRow>
          <FixedSpaceColumn>
            <Label>{t.applications.editor.contactInfo.personFirstName}</Label>
            <InputField
              value={formData.nonCaretakerFirstName || ''}
              onChange={(value) =>
                updateFormData({
                  nonCaretakerFirstName: value.length > 0 ? value : undefined
                })
              }
              placeholder={
                t.applications.editor.contactInfo.firstNamePlaceholder
              }
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{t.applications.editor.contactInfo.personLastName}</Label>
            <InputField
              value={formData.nonCaretakerLastName || ''}
              onChange={(value) =>
                updateFormData({
                  nonCaretakerLastName: value.length > 0 ? value : undefined
                })
              }
              placeholder={
                t.applications.editor.contactInfo.lastNamePlaceholder
              }
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn>
            <Label>{t.applications.editor.contactInfo.personSSN}</Label>
            <InputField
              value={formData.nonCaretakerSSN || ''}
              onChange={(value) =>
                updateFormData({
                  nonCaretakerSSN: value.length > 0 ? value : undefined
                })
              }
              placeholder={t.applications.editor.contactInfo.ssnPlaceholder}
            />
          </FixedSpaceColumn>
        </FixedSpaceRow>
      )}
      <hr />
      <H3>{t.applications.editor.contactInfo.otherChildrenTitle}</H3>
      <P>{t.applications.editor.contactInfo.otherChildrenInfo}</P>
      <P>{t.applications.editor.contactInfo.otherChildrenChoiceInfo}</P>
      {formData.otherChildren.map((child) => (
        <Checkbox
          key={`${child.firstName} ${child.lastName}`}
          checked={false}
          label={`${child.firstName}`}
        />
      ))}
    </ContentArea>
  )
})
