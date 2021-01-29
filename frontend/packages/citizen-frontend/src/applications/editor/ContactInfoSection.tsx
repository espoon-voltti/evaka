// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useTranslation } from '~localization'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { H3, Label, P } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { ContactInfoFormData } from './ApplicationFormData'
import EditorSection from '~applications/editor/EditorSection'
import AddButton from '@evaka/lib-components/src/atoms/buttons/AddButton'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { faTimes } from '@evaka/lib-icons'
import { useUser } from '~auth/state'
import { ApplicationPersonBasics } from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import { Gap } from '@evaka/lib-components/src/white-space'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import { DatePickerClearableDeprecated } from '@evaka/lib-components/src/molecules/DatePickerDeprecated'

export type ContactInfoProps = {
  formData: ContactInfoFormData
  updateFormData: (update: Partial<ContactInfoFormData>) => void
}

export default React.memo(function ContactInfoSection({
  formData,
  updateFormData
}: ContactInfoProps) {
  const t = useTranslation()
  const user = useUser()
  const [
    childFutureAddressExistsChecked,
    setChildFutureAddressExistsChecked
  ] = useState<boolean>(false)
  const [
    guardianFutureAddressExistsChecked,
    setGuardianFutureAddressExistsChecked
  ] = useState<boolean>(false)
  const [
    guardianFutureAddressEqualsChildFutureAddressChecked,
    setGuardianFutureAddressEqualsChildFutureAddressChecked
  ] = useState<boolean>(false)
  const [
    otherPartnerCheckboxChecked,
    setOtherPartnerCheckboxChecked
  ] = useState<boolean>(false)
  const knownOtherChildren: ApplicationPersonBasics[] = user
    ? user.children.filter((c) => c.socialSecurityNumber !== formData.childSSN)
    : []
  const [knownOtherChildrenChoices, setKnownOtherChildrenChoices] = useState<
    boolean[]
  >(
    knownOtherChildren.map((child) =>
      formData.otherChildren
        .map((c) => c.socialSecurityNumber)
        .includes(child.socialSecurityNumber)
    )
  )
  const isExtraChild = (child: ApplicationPersonBasics) =>
    child.socialSecurityNumber
      ? !user?.children
          .map((child) => child.socialSecurityNumber)
          .includes(child.socialSecurityNumber)
      : true
  const [extraChildren, setExtraChildren] = useState<ApplicationPersonBasics[]>(
    formData.otherChildren.filter(isExtraChild)
  )

  useEffect(() => {
    updateFormData({
      otherChildren: knownOtherChildren
        .filter((c, i) => knownOtherChildrenChoices[i])
        .concat(extraChildren)
    })
  }, [knownOtherChildrenChoices, extraChildren])

  const [
    extraChildrenCheckboxChecked,
    setExtraChildrenCheckboxChecked
  ] = useState<boolean>(!!extraChildren.length)

  return (
    <EditorSection
      title={t.applications.editor.contactInfo.title}
      validationErrors={0}
    >
      <Gap size={'m'} />
      <P
        dangerouslySetInnerHTML={{
          __html: t.applications.editor.contactInfo.info
        }}
      />
      <H3>{t.applications.editor.contactInfo.childInfoTitle}</H3>
      <Gap size={'xs'} />
      <FixedSpaceRow spacing={'XL'}>
        <FixedSpaceFlexWrap>
          <FixedSpaceColumn spacing={'xs'}>
            <Label>{t.applications.editor.contactInfo.childFirstName}</Label>
            <span>{formData.childFirstName}</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xs'}>
            <Label>{t.applications.editor.contactInfo.childLastName}</Label>
            <span>{formData.childLastName}</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xs'}>
            <Label>{t.applications.editor.contactInfo.childSSN}</Label>
            <span>{formData.childSSN}</span>
          </FixedSpaceColumn>
        </FixedSpaceFlexWrap>
      </FixedSpaceRow>
      <Gap size={'s'} />
      <FixedSpaceColumn spacing={'xs'}>
        <Label>{t.applications.editor.contactInfo.homeAddress}</Label>
        <span>{formData.childStreet}</span>
      </FixedSpaceColumn>
      <Gap size={'m'} />
      <Checkbox
        label={t.applications.editor.contactInfo.hasFutureAddress}
        checked={childFutureAddressExistsChecked}
        onChange={(checked) => {
          setChildFutureAddressExistsChecked(checked)
          updateFormData({
            childFutureAddressExists: childFutureAddressExistsChecked
          })
          if (!checked) {
            setGuardianFutureAddressEqualsChildFutureAddressChecked(false)
          }
        }}
      />
      {childFutureAddressExistsChecked && (
        <>
          <Gap size={'m'} />
          <FixedSpaceColumn spacing={'xs'}>
            <Label>{t.applications.editor.contactInfo.moveDate + '*'}</Label>
            <DatePickerClearableDeprecated
              date={formData.childMoveDate}
              onChange={(value) =>
                updateFormData(
                  guardianFutureAddressEqualsChildFutureAddressChecked
                    ? { guardianMoveDate: value, childMoveDate: value }
                    : { childMoveDate: value }
                )
              }
              onCleared={() =>
                updateFormData(
                  guardianFutureAddressEqualsChildFutureAddressChecked
                    ? { guardianMoveDate: null, childMoveDate: null }
                    : { childMoveDate: null }
                )
              }
              placeholder={t.applications.editor.contactInfo.choosePlaceholder}
              type={'short'}
            />
          </FixedSpaceColumn>
          <Gap size={'s'} />
          <FixedSpaceRow spacing={'XL'}>
            <FixedSpaceFlexWrap>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'child-future-street'}>
                  {t.applications.editor.contactInfo.street + '*'}
                </Label>
                <InputField
                  id={'child-future-street'}
                  value={formData.childFutureStreet || ''}
                  onChange={(value) =>
                    updateFormData(
                      guardianFutureAddressEqualsChildFutureAddressChecked
                        ? {
                            childFutureStreet: value,
                            guardianFutureStreet: value
                          }
                        : {
                            childFutureStreet: value
                          }
                    )
                  }
                  placeholder={
                    t.applications.editor.contactInfo.streetPlaceholder
                  }
                  width={'L'}
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'child-future-postal-code'}>
                  {t.applications.editor.contactInfo.postalCode + '*'}
                </Label>
                <InputField
                  id={'child-future-postal-code'}
                  value={formData.childFuturePostalCode || ''}
                  onChange={(value) =>
                    updateFormData(
                      guardianFutureAddressEqualsChildFutureAddressChecked
                        ? {
                            guardianFuturePostalCode: value,
                            childFuturePostalCode: value
                          }
                        : {
                            childFuturePostalCode: value
                          }
                    )
                  }
                  placeholder={
                    t.applications.editor.contactInfo.postalCodePlaceholder
                  }
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'child-future-post-office'}>
                  {t.applications.editor.contactInfo.postOffice + '*'}
                </Label>
                <InputField
                  id={'child-future-post-office'}
                  value={formData.childFuturePostOffice || ''}
                  onChange={(value) =>
                    updateFormData(
                      guardianFutureAddressEqualsChildFutureAddressChecked
                        ? {
                            guardianFuturePostOffice: value,
                            childFuturePostOffice: value
                          }
                        : {
                            childFuturePostOffice: value
                          }
                    )
                  }
                  placeholder={
                    t.applications.editor.contactInfo.municipalityPlaceholder
                  }
                />
              </FixedSpaceColumn>
            </FixedSpaceFlexWrap>
          </FixedSpaceRow>
        </>
      )}
      <Gap size={'s'} />
      <HorizontalLine />
      <H3>{t.applications.editor.contactInfo.guardianInfoTitle}</H3>
      <FixedSpaceRow spacing={'XL'}>
        <FixedSpaceFlexWrap>
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
        </FixedSpaceFlexWrap>
      </FixedSpaceRow>
      <Gap size={'s'} />
      <FixedSpaceColumn spacing={'xs'}>
        <Label>{t.applications.editor.contactInfo.homeAddress}</Label>
        <span>{formData.guardianHomeAddress}</span>
      </FixedSpaceColumn>
      <Gap size={'s'} />
      <FixedSpaceRow spacing={'XL'}>
        <FixedSpaceFlexWrap>
          <FixedSpaceColumn spacing={'xs'}>
            <Label htmlFor={'guardian-phone'}>
              {t.applications.editor.contactInfo.phone + '*'}
            </Label>
            <InputField
              id={'guardian-phone'}
              value={formData.guardianPhone}
              onChange={(value) => updateFormData({ guardianPhone: value })}
              placeholder={t.applications.editor.contactInfo.phone}
            />
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing={'xs'}>
            <Label htmlFor={'guardian-email'}>
              {t.applications.editor.contactInfo.emailAddress}
            </Label>
            <InputField
              id={'guardian-email'}
              value={formData.guardianEmail}
              onChange={(value) => updateFormData({ guardianEmail: value })}
              placeholder={t.applications.editor.contactInfo.email}
              width={'L'}
            />
          </FixedSpaceColumn>
        </FixedSpaceFlexWrap>
      </FixedSpaceRow>
      <Gap size={'m'} />
      <Checkbox
        label={t.applications.editor.contactInfo.hasFutureAddress}
        checked={guardianFutureAddressExistsChecked}
        onChange={(checked) => {
          setGuardianFutureAddressExistsChecked(checked)
          updateFormData({
            guardianFutureAddressExists: guardianFutureAddressExistsChecked
          })
        }}
      />
      {guardianFutureAddressExistsChecked && (
        <>
          <Gap size={'m'} />
          {childFutureAddressExistsChecked && (
            <>
              <Checkbox
                label={
                  t.applications.editor.contactInfo
                    .guardianFutureAddressEqualsChildFutureAddress
                }
                checked={guardianFutureAddressEqualsChildFutureAddressChecked}
                onChange={(checked) => {
                  setGuardianFutureAddressEqualsChildFutureAddressChecked(
                    checked
                  )
                  updateFormData({
                    guardianMoveDate: formData.childMoveDate,
                    guardianFutureStreet: formData.childFutureStreet,
                    guardianFuturePostalCode: formData.childFuturePostalCode,
                    guardianFuturePostOffice: formData.childFuturePostOffice
                  })
                }}
              />
              <Gap size={'m'} />
            </>
          )}
          <FixedSpaceColumn spacing={'xs'}>
            <Label>{t.applications.editor.contactInfo.moveDate + '*'}</Label>
            <DatePickerClearableDeprecated
              date={formData.guardianMoveDate}
              onChange={(value) => updateFormData({ guardianMoveDate: value })}
              onCleared={() => updateFormData({ guardianMoveDate: null })}
              placeholder={t.applications.editor.contactInfo.choosePlaceholder}
              disabled={guardianFutureAddressEqualsChildFutureAddressChecked}
              type={'short'}
            />
          </FixedSpaceColumn>
          <Gap size={'s'} />
          <FixedSpaceRow spacing={'XL'}>
            <FixedSpaceFlexWrap>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'guardian-future-street'}>
                  {t.applications.editor.contactInfo.street + '*'}
                </Label>
                <InputField
                  id={'guardian-future-street'}
                  value={formData.guardianFutureStreet || ''}
                  onChange={(value) =>
                    updateFormData({
                      guardianFutureStreet: value
                    })
                  }
                  placeholder={
                    t.applications.editor.contactInfo.streetPlaceholder
                  }
                  readonly={
                    guardianFutureAddressEqualsChildFutureAddressChecked
                  }
                  width={'L'}
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'guardian-future-postal-code'}>
                  {t.applications.editor.contactInfo.postalCode + '*'}
                </Label>
                <InputField
                  id={'guardian-future-postal-code'}
                  value={formData.guardianFuturePostalCode || ''}
                  onChange={(value) =>
                    updateFormData({
                      guardianFuturePostalCode: value
                    })
                  }
                  placeholder={
                    t.applications.editor.contactInfo.postalCodePlaceholder
                  }
                  readonly={
                    guardianFutureAddressEqualsChildFutureAddressChecked
                  }
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'guardian-future-post-office'}>
                  {t.applications.editor.contactInfo.postOffice + '*'}
                </Label>
                <InputField
                  id={'guardian-future-post-office'}
                  value={formData.guardianFuturePostOffice || ''}
                  onChange={(value) =>
                    updateFormData({
                      guardianFuturePostOffice: value
                    })
                  }
                  placeholder={
                    t.applications.editor.contactInfo.municipalityPlaceholder
                  }
                  readonly={
                    guardianFutureAddressEqualsChildFutureAddressChecked
                  }
                />
              </FixedSpaceColumn>
            </FixedSpaceFlexWrap>
          </FixedSpaceRow>
        </>
      )}
      <HorizontalLine />
      <H3>{t.applications.editor.contactInfo.secondGuardianInfoTitle}</H3>
      <FixedSpaceColumn spacing={'xs'}>
        <p>{t.applications.editor.contactInfo.secondGuardianInfo}</p>
      </FixedSpaceColumn>
      <HorizontalLine />
      <H3>{t.applications.editor.contactInfo.otherPartnerTitle}</H3>
      <Gap size={'s'} />
      <Checkbox
        label={t.applications.editor.contactInfo.otherPartnerCheckboxLabel}
        checked={otherPartnerCheckboxChecked}
        onChange={(checked) => {
          setOtherPartnerCheckboxChecked(checked)
          updateFormData({
            otherPartnerInSameAddress: otherPartnerCheckboxChecked
          })
        }}
      />
      {otherPartnerCheckboxChecked && (
        <>
          <Gap size={'m'} />
          <FixedSpaceRow spacing={'XL'}>
            <FixedSpaceFlexWrap>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'other-partner-first-name'}>
                  {t.applications.editor.contactInfo.personFirstName + '*'}
                </Label>
                <InputField
                  id={'other-partner-first-name'}
                  value={formData.otherPartnerFirstName || ''}
                  onChange={(value) =>
                    updateFormData({
                      otherPartnerFirstName: value
                    })
                  }
                  placeholder={
                    t.applications.editor.contactInfo.firstNamePlaceholder
                  }
                  width={'L'}
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'other-partner-last-name'}>
                  {t.applications.editor.contactInfo.personLastName + '*'}
                </Label>
                <InputField
                  id={'other-partner-last-name'}
                  value={formData.otherPartnerLastName || ''}
                  onChange={(value) =>
                    updateFormData({
                      otherPartnerLastName: value
                    })
                  }
                  placeholder={
                    t.applications.editor.contactInfo.lastNamePlaceholder
                  }
                  width={'m'}
                />
              </FixedSpaceColumn>
              <FixedSpaceColumn spacing={'xs'}>
                <Label htmlFor={'other-partner-ssn'}>
                  {t.applications.editor.contactInfo.personSSN + '*'}
                </Label>
                <InputField
                  id={'other-partner-ssn'}
                  value={formData.otherPartnerSSN || ''}
                  onChange={(value) =>
                    updateFormData({
                      otherPartnerSSN: value
                    })
                  }
                  placeholder={t.applications.editor.contactInfo.ssnPlaceholder}
                />
              </FixedSpaceColumn>
            </FixedSpaceFlexWrap>
          </FixedSpaceRow>
        </>
      )}
      <HorizontalLine />
      <H3>{t.applications.editor.contactInfo.otherChildrenTitle}</H3>
      <P>{t.applications.editor.contactInfo.otherChildrenInfo}</P>
      <P>{t.applications.editor.contactInfo.otherChildrenChoiceInfo}</P>
      {knownOtherChildren.map((child, index) => (
        <React.Fragment key={`known-other-child-${index}`}>
          <Gap size={'s'} />
          <Checkbox
            label={`${child.firstName || ''} ${child.lastName || ''}, ${
              child.socialSecurityNumber || ''
            }`}
            checked={knownOtherChildrenChoices[index]}
            onChange={() =>
              setKnownOtherChildrenChoices(
                knownOtherChildrenChoices.map((choice, i) =>
                  i === index ? !choice : choice
                )
              )
            }
          />
        </React.Fragment>
      ))}
      <Gap size={'s'} />
      <Checkbox
        checked={extraChildrenCheckboxChecked}
        label={t.applications.editor.contactInfo.areExtraChildren}
        onChange={(checked) => {
          setExtraChildrenCheckboxChecked(checked)
          if (extraChildren.length === 0) {
            setExtraChildren(
              extraChildren.concat([
                {
                  firstName: '',
                  lastName: '',
                  socialSecurityNumber: ''
                }
              ])
            )
          }
        }}
      />
      {extraChildrenCheckboxChecked && (
        <>
          <Gap size={'m'} />
          {extraChildren.map((child, index) => (
            <React.Fragment key={`extra-child-${index}`}>
              <Gap size={'s'} />
              <FixedSpaceRow spacing={'XL'}>
                <FixedSpaceFlexWrap>
                  <FixedSpaceColumn spacing={'xs'}>
                    <Label htmlFor={`extra-child-first-name-${index}`}>
                      {t.applications.editor.contactInfo.childFirstName + '*'}
                    </Label>
                    <InputField
                      id={`extra-child-first-name-${index}`}
                      value={child.firstName}
                      onChange={(value) => {
                        setExtraChildren(
                          extraChildren.map((c, i) =>
                            i === index ? { ...c, firstName: value } : c
                          )
                        )
                      }}
                      placeholder={
                        t.applications.editor.contactInfo.firstNamePlaceholder
                      }
                      width={'L'}
                    />
                  </FixedSpaceColumn>
                  <FixedSpaceColumn spacing={'xs'}>
                    <Label htmlFor={`extra-child-last-name-${index}`}>
                      {t.applications.editor.contactInfo.childLastName + '*'}
                    </Label>
                    <InputField
                      id={`extra-child-last-name-${index}`}
                      value={child.lastName}
                      onChange={(value) => {
                        setExtraChildren(
                          extraChildren.map((c, i) =>
                            i === index ? { ...c, lastName: value } : c
                          )
                        )
                      }}
                      placeholder={
                        t.applications.editor.contactInfo.lastNamePlaceholder
                      }
                    />
                  </FixedSpaceColumn>
                  <FixedSpaceColumn spacing={'xs'}>
                    <Label htmlFor={`extra-child-ssn-${index}`}>
                      {t.applications.editor.contactInfo.childSSN + '*'}
                    </Label>
                    <InputField
                      id={`extra-child-ssn-${index}`}
                      value={child.socialSecurityNumber || ''}
                      onChange={(value) => {
                        setExtraChildren(
                          extraChildren.map((c, i) =>
                            i === index
                              ? { ...c, socialSecurityNumber: value }
                              : c
                          )
                        )
                      }}
                      placeholder={
                        t.applications.editor.contactInfo.ssnPlaceholder
                      }
                    />
                  </FixedSpaceColumn>
                  <FixedSpaceColumn>
                    <span />
                    <span />
                    <InlineButton
                      icon={faTimes}
                      text={t.applications.editor.contactInfo.remove}
                      onClick={() => {
                        setExtraChildren(
                          extraChildren.filter((c, i) => i !== index)
                        )
                      }}
                    />
                  </FixedSpaceColumn>
                </FixedSpaceFlexWrap>
              </FixedSpaceRow>
            </React.Fragment>
          ))}
          <Gap size={'s'} />
          <AddButton
            text={t.applications.editor.contactInfo.addChild}
            onClick={() => {
              setExtraChildren(
                extraChildren.concat([
                  {
                    firstName: '',
                    lastName: '',
                    socialSecurityNumber: ''
                  }
                ])
              )
            }}
          />
        </>
      )}
    </EditorSection>
  )
})
