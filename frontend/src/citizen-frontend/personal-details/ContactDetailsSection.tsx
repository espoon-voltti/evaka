// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import type { Failure } from 'lib-common/api'
import { boolean, string } from 'lib-common/form/fields'
import { chained, object, validated } from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
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
import { Chip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { InformationText, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { faCheck, faCheckCircle, faExclamation, faLockAlt } from 'lib-icons'

import type { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import {
  DataRow,
  DataRowLabel,
  DataRowValue,
  EditableSectionHeader
} from './components'
import type { EmailVerificationProblem } from './emailVerification'
import { getEmailVerificationProblem } from './emailVerification'
import {
  sendEmailVerificationCodeMutation,
  updatePersonalDetailsMutation,
  verifyEmailMutation
} from './queries'

const EmailVerificationStatus = styled.div`
  display: flex;
  flex-direction: column;
`

const CodeAndConfirmRow = styled.div`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.s};

  @media (max-width: ${tabletMin}) {
    flex-direction: column;
    align-items: stretch;
  }
`

interface Props {
  user: User
  emailVerificationStatus: EmailVerificationStatusResponse
  reloadUser: () => void
}

const contactDetailsForm = object({
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
  ),
  phone: validated(string(), requiredPhoneNumber),
  backupPhone: validated(string(), optionalPhoneNumber)
})

export default React.memo(function ContactDetailsSection({
  user,
  emailVerificationStatus,
  reloadUser
}: Props) {
  const t = useTranslation()

  const [editMode, setEditMode] = useBoolean(false)

  const canEdit = user.authLevel === 'STRONG'
  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  const problem = getEmailVerificationProblem(user, emailVerificationStatus)

  const initialFormState = {
    email: {
      email: user.email ?? '',
      noEmail: !user.email
    },
    phone: user.phone,
    backupPhone: user.backupPhone
  }

  const form = useForm(
    contactDetailsForm,
    () => initialFormState,
    t.validationErrors
  )

  const emailObjectState = useFormField(form, 'email')
  const emailState = useFormField(emailObjectState, 'email')
  const noEmailState = useFormField(emailObjectState, 'noEmail')
  const phoneState = useFormField(form, 'phone')
  const backupPhoneState = useFormField(form, 'backupPhone')

  return (
    <div data-qa="contact-details-section">
      <EditableSectionHeader
        title={t.personalDetails.detailsSection.contactInfo}
        editing={editMode}
        onStartEditing={setEditMode.on}
        onCancel={() => {
          setEditMode.off()
          form.set(initialFormState)
        }}
        mutation={updatePersonalDetailsMutation}
        onSave={() => ({
          body: { preferredName: null, ...form.value() }
        })}
        onSaveSuccess={() => {
          reloadUser()
          setEditMode.off()
        }}
        saveDisabled={!form.isValid()}
        canEdit={canEdit}
        navigateToLogin={navigateToLogin}
      />

      <Gap $size="xs" />

      <DataRow $highlighted={!editMode && problem !== undefined}>
        <DataRowLabel htmlFor="email-input">
          {t.personalDetails.detailsSection.email}
        </DataRowLabel>
        <DataRowValue>
          {editMode ? (
            <FixedSpaceColumn $spacing="xs">
              <InputFieldF
                id="email-input"
                type="email"
                width="m"
                bind={emailState}
                info={emailObjectState.inputInfo()}
                required={!noEmailState.state}
                readonly={noEmailState.state}
                hideErrorsBeforeTouched
                placeholder={t.personalDetails.detailsSection.email}
                autoFocus={true}
                data-qa="email"
              />
              <CheckboxF
                label={t.personalDetails.detailsSection.noEmail}
                bind={noEmailState}
                data-qa="no-email"
              />
            </FixedSpaceColumn>
          ) : (
            <>
              <EmailVerificationStatus>
                <span data-qa="email" translate="no">
                  {user.email || '-'}
                </span>
                <Gap $size="xs" />
                {!!user.email && (
                  <EmailVerificationStatusView
                    emailVerificationStatus={emailVerificationStatus}
                    problem={problem}
                    canEdit={canEdit}
                    navigateToLogin={navigateToLogin}
                    reloadUser={reloadUser}
                  />
                )}
              </EmailVerificationStatus>
              {problem?.type === 'mismatch' &&
                !emailVerificationStatus.latestVerification && (
                  <>
                    <AlertBox
                      noMargin
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
                    <Gap $size="xs" />
                    <VerificationActionButton
                      text={t.personalDetails.detailsSection.updateUsername(
                        problem.email
                      )}
                      dataQa="update-username"
                      canEdit={canEdit}
                      navigateToLogin={navigateToLogin}
                    />
                  </>
                )}
            </>
          )}
        </DataRowValue>
      </DataRow>
      <DataRow>
        <DataRowLabel htmlFor="phone-input">
          {t.personalDetails.detailsSection.phone}
          {editMode && <span> *</span>}
        </DataRowLabel>
        <DataRowValue>
          {editMode ? (
            <InputFieldF
              required
              id="phone-input"
              type="tel"
              width="m"
              bind={phoneState}
              hideErrorsBeforeTouched
              placeholder="0401234567"
              data-qa="phone"
            />
          ) : (
            <span data-qa="phone">{user.phone || '-'}</span>
          )}
        </DataRowValue>
      </DataRow>
      <DataRow>
        <DataRowLabel htmlFor="backup-phone-input">
          {t.personalDetails.detailsSection.backupPhone}
        </DataRowLabel>
        <DataRowValue>
          {editMode ? (
            <InputFieldF
              id="backup-phone-input"
              type="tel"
              width="m"
              bind={backupPhoneState}
              hideErrorsBeforeTouched
              placeholder={
                t.personalDetails.detailsSection.backupPhonePlaceholder
              }
              data-qa="backup-phone"
            />
          ) : (
            <span data-qa="backup-phone">{user.backupPhone || '-'}</span>
          )}
        </DataRowValue>
      </DataRow>
    </div>
  )
})

const EmailVerificationStatusView = React.memo(
  function EmailVerificationStatusView({
    emailVerificationStatus,
    problem,
    canEdit,
    navigateToLogin,
    reloadUser
  }: {
    emailVerificationStatus: EmailVerificationStatusResponse
    problem: EmailVerificationProblem | undefined
    canEdit: boolean
    navigateToLogin: () => void
    reloadUser: () => void
  }) {
    const t = useTranslation()

    if (emailVerificationStatus.latestVerification) {
      return (
        <>
          {problem?.type === 'unverified' && <UnverifiedChip />}
          <HorizontalLine $slim />
          <EmailVerificationForm
            reloadUser={reloadUser}
            usernameMismatch={problem?.type === 'mismatch'}
            verification={emailVerificationStatus.latestVerification}
          />
        </>
      )
    }
    if (problem?.type === 'unverified') {
      return (
        <>
          <UnverifiedChip />
          <Gap $size="xs" />
          {/* block wrapper so the inline button doesn't stretch and
              center its label in the flex column */}
          <div>
            <VerificationActionButton
              appearance="inline"
              text={t.personalDetails.detailsSection.sendVerificationCode}
              dataQa="send-verification-code"
              canEdit={canEdit}
              navigateToLogin={navigateToLogin}
            />
          </div>
        </>
      )
    }
    if (!problem) {
      return (
        <Chip
          colorPalette="green"
          icon={faCheck}
          label={t.personalDetails.detailsSection.emailVerified}
          data-qa="verified-email-status"
          iconCircle
          size="small"
        />
      )
    }
    return null
  }
)

const VerificationActionButton = React.memo(function VerificationActionButton({
  text,
  dataQa,
  canEdit,
  navigateToLogin,
  appearance
}: {
  text: string
  dataQa: string
  canEdit: boolean
  navigateToLogin: () => void
  appearance?: 'inline'
}) {
  return canEdit ? (
    <MutateButton
      data-qa={dataQa}
      appearance={appearance}
      text={text}
      mutation={sendEmailVerificationCodeMutation}
      onClick={() => undefined}
    />
  ) : (
    <Button
      data-qa={dataQa}
      appearance={appearance}
      text={text}
      icon={faLockAlt}
      onClick={navigateToLogin}
    />
  )
})

const UnverifiedChip = React.memo(function UnverifiedChip() {
  const t = useTranslation()
  return (
    <Chip
      colorPalette="orange"
      label={t.personalDetails.detailsSection.emailUnverified}
      data-qa="unverified-email-status"
      iconCircle
      icon={faExclamation}
      size="small"
    />
  )
})

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
      <div>
        <FontAwesomeIcon icon={faCheckCircle} color={colors.status.success} />
      </div>
      <div>
        {usernameMismatch
          ? t.changeUsername.codeSent(verification)
          : t.verifyEmail.codeSent(verification)}
      </div>
      <Label htmlFor="verification-code-input">{`${t.verificationForm}`}</Label>
      <CodeAndConfirmRow>
        <InputFieldF
          id="verification-code-input"
          data-qa="verification-code-field"
          bind={verificationCode}
          width="full"
          hideErrorsBeforeTouched={true}
        />
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
      </CodeAndConfirmRow>
      {isUsernameConflict && (
        <AlertBox
          message={i18n.personalDetails.loginDetailsSection.usernameConflict(
            verification.email
          )}
        />
      )}
      <InformationText>
        {t.codeNotReceived} {t.codeNotReceivedInfo}
      </InformationText>
    </FixedSpaceColumn>
  )
})
