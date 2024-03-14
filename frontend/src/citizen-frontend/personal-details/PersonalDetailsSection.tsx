// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faLockAlt, faPen } from 'Icons'
import React, { useCallback } from 'react'

import { boolean, string } from 'lib-common/form/fields'
import {
  chained,
  object,
  oneOf,
  required,
  validated
} from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
import { StateOf, ValidationSuccess } from 'lib-common/form/types'
import {
  optionalPhoneNumber,
  requiredEmail,
  requiredPhoneNumber
} from 'lib-common/form/validators'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { EditButtonRow, MandatoryValueMissingWarning } from './components'
import { updatePersonalDetailsMutation } from './queries'

export interface Props {
  user: User
  reloadUser: () => void
}

export default React.memo(function PersonalDetailsSection({
  user,
  reloadUser
}: Props) {
  const t = useTranslation()

  const [editing, setEditing] = useBoolean(false)
  const [emailInfo, setEmailInfo] = useBoolean(false)
  const form = useForm(
    personForm,
    () => initialFormState(user),
    t.validationErrors
  )
  const preferredNameState = useFormField(form, 'preferredName')
  const phoneState = useFormField(form, 'phone')
  const backupPhoneState = useFormField(form, 'backupPhone')

  const emailObjectState = useFormField(form, 'email')
  const emailState = useFormField(emailObjectState, 'email')
  const noEmailState = useFormField(emailObjectState, 'noEmail')

  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  const formWrapper = (children: React.ReactNode) =>
    editing ? (
      <form>
        {children}
        <FixedSpaceRow justifyContent="flex-end">
          <Button
            type="button"
            text={t.common.cancel}
            onClick={() => {
              setEditing.off()
              form.set(initialFormState(user))
            }}
          />
          <MutateButton
            primary
            type="submit"
            text={t.common.save}
            mutation={updatePersonalDetailsMutation}
            onClick={() => ({
              body: form.value()
            })}
            onSuccess={() => {
              reloadUser()
              setEditing.off()
            }}
            disabled={!form.isValid()}
            data-qa="save"
          />
        </FixedSpaceRow>
      </form>
    ) : (
      <>
        {children}
        <Gap size="XL" />
      </>
    )

  const getAlertMessage = (
    email: string | null,
    phone: string | null
  ): string => {
    const alerts = []
    if (email === null)
      alerts.push(t.personalDetails.detailsSection.noEmailAlert)
    if (!phone) alerts.push(t.personalDetails.detailsSection.noPhoneAlert)
    return alerts.join('. ').concat('.')
  }

  const {
    firstName,
    lastName,
    preferredName,
    streetAddress,
    postalCode,
    postOffice,
    phone,
    backupPhone,
    email,
    authLevel
  } = user

  const canEdit = authLevel === 'STRONG'

  return (
    <div data-qa="personal-details-section">
      {(email === null || !phone) && (
        <AlertBox
          message={getAlertMessage(email, phone)}
          data-qa="missing-email-or-phone-box"
        />
      )}
      <EditButtonRow>
        <InlineButton
          text={t.common.edit}
          icon={canEdit ? faPen : faLockAlt}
          onClick={
            canEdit
              ? () => {
                  form.set(initialFormState(user))
                  setEditing.on()
                }
              : navigateToLogin
          }
          disabled={editing}
          data-qa={canEdit ? 'start-editing' : 'start-editing-login'}
        />
      </EditButtonRow>
      {formWrapper(
        <>
          <ListGrid rowGap="s" columnGap="L" labelWidth="max-content">
            <H2 noMargin>{t.personalDetails.detailsSection.title}</H2>
            <div />
            <Label>{t.personalDetails.detailsSection.name}</Label>
            <div>
              {firstName} {lastName}
            </div>
            <Label>{t.personalDetails.detailsSection.preferredName}</Label>
            {editing ? (
              <SelectF bind={preferredNameState} data-qa="preferred-name" />
            ) : (
              <div data-qa="preferred-name">{preferredName}</div>
            )}
            <H2 noMargin>{t.personalDetails.detailsSection.contactInfo}</H2>
            <div />
            <Label>{t.personalDetails.detailsSection.address}</Label>
            <div>
              {!!streetAddress &&
                `${streetAddress}, ${postalCode} ${postOffice}`}
            </div>
            <Label>{t.personalDetails.detailsSection.phone}</Label>
            <div data-qa="phone">
              {editing ? (
                <InputFieldF
                  type="text"
                  width="m"
                  bind={phoneState}
                  hideErrorsBeforeTouched
                  placeholder="0401234567"
                />
              ) : (
                <div>
                  {phone ? (
                    phone
                  ) : (
                    <MandatoryValueMissingWarning
                      text={t.personalDetails.detailsSection.phoneMissing}
                    />
                  )}
                </div>
              )}
            </div>
            <Label>{t.personalDetails.detailsSection.backupPhone}</Label>
            <div data-qa="backup-phone">
              {editing ? (
                <InputFieldF
                  type="text"
                  width="m"
                  bind={backupPhoneState}
                  hideErrorsBeforeTouched
                  placeholder={
                    t.personalDetails.detailsSection.backupPhonePlaceholder
                  }
                />
              ) : (
                backupPhone
              )}
            </div>
            <Label>{t.personalDetails.detailsSection.email}</Label>
            {editing ? (
              <FixedSpaceColumn spacing="xs">
                <div data-qa="email">
                  <InputFieldF
                    type="text"
                    width="m"
                    bind={emailState}
                    readonly={noEmailState.state}
                    hideErrorsBeforeTouched
                    placeholder={t.personalDetails.detailsSection.email}
                  />
                </div>
                <FixedSpaceRow alignItems="center">
                  <CheckboxF
                    label={t.personalDetails.detailsSection.noEmail}
                    bind={noEmailState}
                    data-qa="no-email"
                  />
                  <InfoButton
                    onClick={setEmailInfo.toggle}
                    aria-label={t.common.openExpandingInfo}
                  />
                </FixedSpaceRow>
              </FixedSpaceColumn>
            ) : (
              <div data-qa="email">
                {email ? (
                  email
                ) : (
                  <MandatoryValueMissingWarning
                    text={t.personalDetails.detailsSection.emailMissing}
                  />
                )}
              </div>
            )}
          </ListGrid>
          {emailInfo && (
            <>
              <ExpandingInfoBox
                info={t.personalDetails.detailsSection.emailInfo}
                close={setEmailInfo.off}
              />
              <Gap size="s" />
            </>
          )}
        </>
      )}
    </div>
  )
})

const personForm = object({
  preferredName: required(oneOf<string>()),
  phone: validated(string(), requiredPhoneNumber),
  backupPhone: validated(string(), optionalPhoneNumber),
  email: chained(
    object({
      email: validated(string(), requiredEmail),
      noEmail: boolean()
    }),
    (form, state) => {
      if (state.noEmail) {
        return ValidationSuccess.of('')
      }
      return form.shape().email.validate(state.email)
    }
  )
})

function initialFormState(user: User): StateOf<typeof personForm> {
  const firstNames = user.firstName.split(' ')

  const preferredNameOptions = firstNames.map((name) => ({
    value: name,
    label: name,
    domValue: name
  }))

  return {
    preferredName: {
      options: preferredNameOptions,
      domValue: user.preferredName || (firstNames[0] ?? '')
    },
    phone: user.phone,
    backupPhone: user.backupPhone,
    email: {
      email: user.email ?? '',
      noEmail: false
    }
  }
}
