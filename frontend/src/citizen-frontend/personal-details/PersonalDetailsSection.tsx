// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import type { Failure } from 'lib-common/api'
import { boolean, string } from 'lib-common/form/fields'
import {
  chained,
  object,
  oneOf,
  required,
  validated
} from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
import type { StateOf } from 'lib-common/form/types'
import { ValidationSuccess } from 'lib-common/form/types'
import {
  optionalPhoneNumber,
  regexp,
  requiredEmail,
  requiredPhoneNumber
} from 'lib-common/form/validators'
import type {
  EmailVerification,
  EmailVerificationStatusResponse
} from 'lib-common/generated/api-types/pis'
import { NotificationsContext } from 'lib-components/Notifications'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { H2, Label, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faCheckCircle,
  faExclamationTriangle,
  faLockAlt,
  faPen
} from 'lib-icons'

import type { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import {
  EditButtonRow,
  FullRow,
  Grid,
  MandatoryValueMissingWarning
} from './components'
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
  const [emailEditInfo, setEmailEditInfo] = useBoolean(false)
  const [emailInfo, { toggle: toggleEmailInfo, off: closeEmailInfo }] =
    useBoolean(false)
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

  const problem:
    | { type: 'mismatch' | 'unverified'; email: string }
    | undefined = emailVerificationStatus.email
    ? user.weakLoginUsername
      ? user.weakLoginUsername !== emailVerificationStatus.email.toLowerCase()
        ? { type: 'mismatch', email: emailVerificationStatus.email }
        : undefined
      : emailVerificationStatus.email !== emailVerificationStatus.verifiedEmail
        ? { type: 'unverified', email: emailVerificationStatus.email }
        : undefined
    : undefined

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
          <Grid>
            <H2 noMargin>{t.personalDetails.detailsSection.title}</H2>
            <div />
            <Label>{t.personalDetails.detailsSection.name}</Label>
            <div>
              <PersonName person={user} format="First Last" />
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
                    onClick={setEmailEditInfo.toggle}
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
                      <Gap horizontal size="xs" />
                      <InfoButton
                        onClick={toggleEmailInfo}
                        aria-label={t.common.openExpandingInfo}
                      />
                    </>
                  ) : (
                    <MandatoryValueMissingWarning
                      text={t.personalDetails.detailsSection.emailMissing}
                    />
                  )}
                </div>
                {emailInfo && (
                  <FullRow>
                    <ExpandingInfoBox
                      info={t.personalDetails.detailsSection.contactEmailInfo}
                      close={closeEmailInfo}
                    />
                  </FullRow>
                )}
                <div />
                {!!email && (
                  <div>
                    <Gap size="xs" />
                    {emailVerificationStatus.latestVerification ? (
                      <EmailVerificationForm
                        reloadUser={reloadUser}
                        usernameMismatch={problem?.type === 'mismatch'}
                        verification={
                          emailVerificationStatus.latestVerification
                        }
                      />
                    ) : problem?.type === 'unverified' ? (
                      <div>
                        <div data-qa="unverified-email-status">
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
                        {canEdit ? (
                          <MutateButton
                            data-qa="send-verification-code"
                            appearance="inline"
                            text={
                              t.personalDetails.detailsSection
                                .sendVerificationCode
                            }
                            mutation={sendEmailVerificationCodeMutation}
                            onClick={() => undefined}
                          />
                        ) : (
                          <Button
                            data-qa="send-verification-code"
                            appearance="inline"
                            text={
                              t.personalDetails.detailsSection
                                .sendVerificationCode
                            }
                            icon={faLockAlt}
                            onClick={navigateToLogin}
                          />
                        )}
                      </div>
                    ) : (
                      !problem && (
                        <div data-qa="verified-email-status">
                          <FontAwesomeIcon
                            icon={faCheckCircle}
                            color={colors.status.success}
                          />
                          <Gap horizontal size="xs" />
                          {t.personalDetails.detailsSection.emailVerified}
                        </div>
                      )
                    )}
                  </div>
                )}
              </>
            )}
          </Grid>
          {!editing &&
            problem?.type === 'mismatch' &&
            !emailVerificationStatus.latestVerification && (
              <FullRow>
                <AlertBox
                  message={
                    <>
                      {
                        t.personalDetails.detailsSection.updateUsernameAlert
                          .usernameMismatch
                      }
                      <br />
                      {t.personalDetails.detailsSection.updateUsernameAlert.suggestedAction(
                        problem.email
                      )}
                    </>
                  }
                />
                {canEdit ? (
                  <MutateButton
                    data-qa="update-username"
                    text={t.personalDetails.detailsSection.updateUsername(
                      problem.email
                    )}
                    mutation={sendEmailVerificationCodeMutation}
                    onClick={() => undefined}
                  />
                ) : (
                  <Button
                    data-qa="update-username"
                    text={t.personalDetails.detailsSection.updateUsername(
                      problem.email
                    )}
                    icon={faLockAlt}
                    onClick={navigateToLogin}
                  />
                )}
              </FullRow>
            )}
          {emailEditInfo && (
            <>
              <ExpandingInfoBox
                info={t.personalDetails.detailsSection.emailInfo}
                close={setEmailEditInfo.off}
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
  verificationCode: validated(string(), regexp(/[0-9]{6}/, 'format'))
})

const EmailVerificationForm = React.memo(function EmailVerificationForm({
  reloadUser,
  verification,
  usernameMismatch
}: {
  reloadUser: () => void
  verification: EmailVerification
  usernameMismatch: boolean
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.detailsSection

  const { addTimedNotification } = useContext(NotificationsContext)
  const [showInfo, { toggle: toggleInfo, off: closeInfo }] = useBoolean(false)

  const form = useForm(
    emailVerificationForm,
    () => ({ verificationCode: '' }),
    i18n.validationErrors
  )
  const verificationCode = useFormField(form, 'verificationCode')

  const [isUsernameConflict, setUsernameConflict] = useState<boolean>(false)
  const onFailure = useCallback(
    (failure: Failure<unknown>) => {
      setUsernameConflict(failure.statusCode === 409)
    },
    [setUsernameConflict]
  )

  return (
    <FixedSpaceColumn>
      <LabelLike>
        {usernameMismatch ? t.changeUsername.section : t.verifyEmail.section}
      </LabelLike>
      <div>
        <FontAwesomeIcon icon={faCheckCircle} color={colors.status.success} />
        <Gap horizontal size="xs" />
        {usernameMismatch
          ? t.changeUsername.codeSent(verification)
          : t.verifyEmail.codeSent(verification)}
      </div>
      <LabelLike>{`${t.verificationForm}*`}</LabelLike>
      <FixedSpaceRow alignItems="center">
        <InputFieldF
          data-qa="verification-code-field"
          bind={verificationCode}
          width="L"
          hideErrorsBeforeTouched={true}
        />
        <Gap horizontal size="xs" />
        {t.codeNotReceived}
        <InlineInfoButton
          onClick={toggleInfo}
          aria-label={i18n.common.openExpandingInfo}
        />
      </FixedSpaceRow>
      {showInfo && (
        <ExpandingInfoBox
          width="full"
          info={t.codeNotReceivedInfo}
          close={closeInfo}
        />
      )}
      {isUsernameConflict && (
        <AlertBox
          message={i18n.personalDetails.loginDetailsSection.usernameConflict(
            verification.email
          )}
        />
      )}
      <Gap size="m" />
      <MutateButton
        data-qa="verify-email"
        primary
        disabled={!form.isValid() || isUsernameConflict}
        text={t.confirmVerification}
        mutation={verifyEmailMutation}
        onClick={() => ({
          body: {
            id: verification.id,
            code: form.value().verificationCode
          }
        })}
        onSuccess={() => {
          reloadUser()
          addTimedNotification({
            children: usernameMismatch
              ? t.changeUsername.toast
              : t.verifyEmail.toast
          })
        }}
        onFailure={onFailure}
      />
    </FixedSpaceColumn>
  )
})
