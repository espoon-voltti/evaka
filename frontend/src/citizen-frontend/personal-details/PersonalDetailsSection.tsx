// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext } from 'react'
import styled from 'styled-components'

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
  regexp,
  requiredEmail,
  requiredPhoneNumber
} from 'lib-common/form/validators'
import {
  EmailVerification,
  EmailVerificationStatusResponse
} from 'lib-common/generated/api-types/pis'
import { NotificationsContext } from 'lib-components/Notifications'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
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
import { H2, Label, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faCheckCircle,
  faExclamationTriangle,
  faLockAlt,
  faPen
} from 'lib-icons'

import { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { EditButtonRow, MandatoryValueMissingWarning } from './components'
import {
  sendEmailVerificationCodeMutation,
  updatePersonalDetailsMutation,
  verifyEmailMutation
} from './queries'

export interface Props {
  user: User
  emailVerificationStatus: EmailVerificationStatusResponse
  reloadUser: () => void
}

export default React.memo(function PersonalDetailsSection({
  user,
  emailVerificationStatus,
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

  const isEmailVerified = !!(
    emailVerificationStatus.email &&
    emailVerificationStatus.email === emailVerificationStatus.verifiedEmail
  )

  const formWrapper = (children: React.ReactNode) =>
    editing ? (
      <form>
        {children}
        <FixedSpaceRow justifyContent="flex-end">
          <LegacyButton
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
        <Button
          appearance="inline"
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
            <div translate="no">
              {firstName} {lastName}
            </div>
            <Label>{t.personalDetails.detailsSection.preferredName}</Label>
            {editing ? (
              <SelectF bind={preferredNameState} data-qa="preferred-name" />
            ) : (
              <div data-qa="preferred-name" translate="no">
                {preferredName}
              </div>
            )}
            <H2 noMargin>{t.personalDetails.detailsSection.contactInfo}</H2>
            <div />
            <Label>{t.personalDetails.detailsSection.address}</Label>
            <div translate="no">
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
              <>
                <div data-qa="email">
                  {email ? (
                    <>
                      <span translate="no">{email}</span>
                      <Gap size="xs" />
                      {isEmailVerified ? (
                        <div>
                          <FontAwesomeIcon
                            icon={faCheckCircle}
                            color={colors.status.success}
                          />
                          <Gap horizontal size="xs" />
                          {t.personalDetails.detailsSection.emailVerified}
                        </div>
                      ) : emailVerificationStatus.latestVerification ? (
                        <EmailVerificationForm
                          verification={
                            emailVerificationStatus.latestVerification
                          }
                        />
                      ) : (
                        <div>
                          <div>
                            <UnverifiedEmailWarning>
                              {t.personalDetails.detailsSection.emailUnverified}
                            </UnverifiedEmailWarning>
                            <Gap horizontal size="xs" />
                            <FontAwesomeIcon
                              icon={faExclamationTriangle}
                              color={colors.status.warning}
                            />
                          </div>
                          <Gap size="s" />
                          <MutateButton
                            appearance="inline"
                            text={
                              t.personalDetails.detailsSection
                                .sendVerificationCode
                            }
                            mutation={sendEmailVerificationCodeMutation}
                            onClick={() => undefined}
                          />
                        </div>
                      )}
                    </>
                  ) : (
                    <MandatoryValueMissingWarning
                      text={t.personalDetails.detailsSection.emailMissing}
                    />
                  )}
                </div>
              </>
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
      noEmail: !user.email
    }
  }
}

const UnverifiedEmailWarning = styled.span`
  color: ${({ theme }) => theme.colors.status.warning};
`

const emailVerificationForm = object({
  verificationCode: validated(string(), regexp(/[0-9]{8}/, 'format'))
})

const EmailVerificationForm = React.memo(function EmailVerificationForm({
  verification
}: {
  verification: EmailVerification
}) {
  const t = useTranslation()

  const { addTimedNotification } = useContext(NotificationsContext)

  const form = useForm(
    emailVerificationForm,
    () => ({ verificationCode: '' }),
    t.validationErrors
  )
  const verificationCode = useFormField(form, 'verificationCode')

  return (
    <FixedSpaceColumn>
      <LabelLike>
        {t.personalDetails.detailsSection.verificationSection}
      </LabelLike>
      <div>
        <FontAwesomeIcon icon={faCheckCircle} color={colors.status.success} />
        <Gap horizontal size="xs" />
        {t.personalDetails.detailsSection.verificationCodeSent(verification)}
      </div>
      <LabelLike>{`${t.personalDetails.detailsSection.verificationForm}*`}</LabelLike>
      <InputFieldF
        bind={verificationCode}
        width="L"
        hideErrorsBeforeTouched={true}
      />
      <Gap size="m" />
      <MutateButton
        primary
        disabled={!form.isValid()}
        text={t.personalDetails.detailsSection.confirmVerification}
        mutation={verifyEmailMutation}
        onClick={() => ({
          body: {
            id: verification.id,
            code: form.value().verificationCode
          }
        })}
        onSuccess={() => {
          addTimedNotification({
            children: t.personalDetails.detailsSection.emailVerifiedToast
          })
        }}
      />
    </FixedSpaceColumn>
  )
})
