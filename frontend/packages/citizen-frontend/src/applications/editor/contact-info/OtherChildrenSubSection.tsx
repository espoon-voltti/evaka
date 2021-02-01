// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~localization'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { H3, Label, P } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import AddButton from '@evaka/lib-components/src/atoms/buttons/AddButton'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import { faTimes } from '@evaka/lib-icons'
import { Gap } from '@evaka/lib-components/src/white-space'
import { errorToInputInfo } from '~form-validation'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import { ContactInfoSectionProps } from '~applications/editor/contact-info/ContactInfoSection'

export default React.memo(function OtherChildrenSubSection({
  formData,
  updateFormData,
  errors,
  verificationRequested
}: ContactInfoSectionProps) {
  const t = useTranslation()

  return (
    <>
      <H3>{t.applications.editor.contactInfo.otherChildrenTitle}</H3>
      <P>{t.applications.editor.contactInfo.otherChildrenInfo}</P>
      <P>{t.applications.editor.contactInfo.otherChildrenChoiceInfo}</P>
      {formData.vtjSiblings.map((child, index) => (
        <React.Fragment key={`known-other-child-${index}`}>
          <Gap size={'s'} />
          <Checkbox
            label={`${child.firstName || ''} ${child.lastName || ''}, ${
              child.socialSecurityNumber || ''
            }`}
            checked={child.selected}
            onChange={(checked) =>
              updateFormData({
                vtjSiblings: formData.vtjSiblings.map((old) =>
                  old.socialSecurityNumber === child.socialSecurityNumber
                    ? {
                        ...old,
                        selected: checked
                      }
                    : old
                )
              })
            }
          />
        </React.Fragment>
      ))}
      <Gap size={'s'} />
      <Checkbox
        checked={formData.otherChildrenExists}
        dataQa={'otherChildrenExists-input'}
        label={t.applications.editor.contactInfo.areExtraChildren}
        onChange={(checked) => {
          updateFormData({
            otherChildrenExists: checked
          })
          if (formData.otherChildren.length === 0) {
            updateFormData({
              otherChildren: [
                {
                  firstName: '',
                  lastName: '',
                  socialSecurityNumber: ''
                }
              ]
            })
          }
        }}
      />
      {formData.otherChildrenExists && (
        <>
          <Gap size={'m'} />
          <FixedSpaceColumn spacing={'L'}>
            {formData.otherChildren.map((child, index) => (
              <AdaptiveFlex breakpoint="1130px" key={`extra-child-${index}`}>
                <FixedSpaceColumn spacing={'xs'}>
                  <Label htmlFor={`extra-child-first-name-${index}`}>
                    {t.applications.editor.contactInfo.childFirstName + ' *'}
                  </Label>
                  <InputField
                    id={`extra-child-first-name-${index}`}
                    value={child.firstName}
                    dataQa={`otherChildren[${index}].firstName-input`}
                    onChange={(value) => {
                      updateFormData({
                        otherChildren: formData.otherChildren.map((old, i) =>
                          i === index ? { ...old, firstName: value } : old
                        )
                      })
                    }}
                    info={errorToInputInfo(
                      errors.otherChildren?.itemErrors[index]?.firstName,
                      t.validationErrors
                    )}
                    hideErrorsBeforeTouched={!verificationRequested}
                    placeholder={
                      t.applications.editor.contactInfo.firstNamePlaceholder
                    }
                    width={'L'}
                  />
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing={'xs'}>
                  <Label htmlFor={`extra-child-last-name-${index}`}>
                    {t.applications.editor.contactInfo.childLastName + ' *'}
                  </Label>
                  <InputField
                    id={`extra-child-last-name-${index}`}
                    value={child.lastName}
                    dataQa={`otherChildren[${index}].lastName-input`}
                    onChange={(value) => {
                      updateFormData({
                        otherChildren: formData.otherChildren.map((old, i) =>
                          i === index ? { ...old, lastName: value } : old
                        )
                      })
                    }}
                    info={errorToInputInfo(
                      errors.otherChildren?.itemErrors[index]?.lastName,
                      t.validationErrors
                    )}
                    hideErrorsBeforeTouched={!verificationRequested}
                    placeholder={
                      t.applications.editor.contactInfo.lastNamePlaceholder
                    }
                    width="m"
                  />
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing={'xs'}>
                  <Label htmlFor={`extra-child-ssn-${index}`}>
                    {t.applications.editor.contactInfo.childSSN + ' *'}
                  </Label>
                  <FixedSpaceRow>
                    <InputField
                      id={`extra-child-ssn-${index}`}
                      value={child.socialSecurityNumber}
                      dataQa={`otherChildren[${index}].socialSecurityNumber-input`}
                      onChange={(value) => {
                        updateFormData({
                          otherChildren: formData.otherChildren.map((old, i) =>
                            i === index
                              ? {
                                  ...old,
                                  socialSecurityNumber: value.toUpperCase()
                                }
                              : old
                          )
                        })
                      }}
                      info={errorToInputInfo(
                        errors.otherChildren?.itemErrors[index]
                          ?.socialSecurityNumber,
                        t.validationErrors
                      )}
                      hideErrorsBeforeTouched={
                        !verificationRequested &&
                        formData.otherChildren[index].socialSecurityNumber
                          .length < 11
                      }
                      placeholder={
                        t.applications.editor.contactInfo.ssnPlaceholder
                      }
                      width="m"
                    />
                    <InlineButton
                      icon={faTimes}
                      text={t.applications.editor.contactInfo.remove}
                      onClick={() => {
                        updateFormData({
                          otherChildren: formData.otherChildren.filter(
                            (c, i) => i !== index
                          )
                        })
                      }}
                    />
                  </FixedSpaceRow>
                </FixedSpaceColumn>
              </AdaptiveFlex>
            ))}
          </FixedSpaceColumn>

          <Gap size={'m'} />
          <AddButton
            text={t.applications.editor.contactInfo.addChild}
            dataQa={'add-other-child'}
            onClick={() => {
              updateFormData({
                otherChildren: [
                  ...formData.otherChildren,
                  {
                    firstName: '',
                    lastName: '',
                    socialSecurityNumber: ''
                  }
                ]
              })
            }}
          />
        </>
      )}
    </>
  )
})
