// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'

import ModalAccessibilityWrapper from 'citizen-frontend/ModalAccessibilityWrapper'
import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import { Button } from 'lib-components/atoms/buttons/Button'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import BaseModal, {
  ModalButtons
} from 'lib-components/molecules/modals/BaseModal'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/citizen'
import { faCheck, faLockAlt } from 'lib-icons'

import { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { FullRow, Grid } from './components'
import { updateWeakLoginCredentialsMutation } from './queries'

export interface Props {
  user: User
  reloadUser: () => void
  emailVerificationStatus: EmailVerificationStatusResponse
}

export default React.memo(function LoginDetailsSection({
  user,
  reloadUser,
  emailVerificationStatus
}: Props) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.loginDetailsSection

  const canEdit = user.authLevel === 'STRONG'

  const isEmailVerified = !!(
    emailVerificationStatus.email &&
    emailVerificationStatus.email === emailVerificationStatus.verifiedEmail
  )

  const [usernameInfo, { toggle: toggleUsernameInfo, off: closeUsernameInfo }] =
    useBoolean(false)

  const [modalOpen, { off: closeModal, on: openModal }] = useBoolean(false)
  const [
    activationSuccessModalOpen,
    { off: closeActivationSuccessModal, on: openActivationSuccessModal }
  ] = useBoolean(false)

  const [infoOpen, { off: closeInfo, toggle: toggleInfo }] = useBoolean(false)

  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  return (
    <div data-qa="login-details-section">
      <Grid>
        <H2 noMargin>{t.title}</H2>
        <div />
        <Label>
          {t.weakLoginUsername}
          {featureFlags.weakLogin ? ' (Keycloak)' : ''}
        </Label>
        <div data-qa="keycloak-email" translate="no">
          {user.keycloakEmail}
        </div>
        {featureFlags.weakLogin && (user.email || user.weakLoginUsername) && (
          <>
            <Label>{t.weakLoginCredentials}</Label>
            <div>
              {user.weakLoginUsername ? (
                <span data-qa="weak-login-enabled">{t.status.enabled}</span>
              ) : (
                <span data-qa="weak-login-disabled">{t.status.disabled}</span>
              )}
              <InlineInfoButton
                onClick={toggleInfo}
                aria-label={i18n.common.openExpandingInfo}
              />
            </div>
            {infoOpen && (
              <FullRow>
                <ExpandingInfoBox info={t.status.info} close={closeInfo} />
              </FullRow>
            )}
            {user.weakLoginUsername ? (
              <>
                <Label>{t.weakLoginUsername}</Label>
                <div>
                  <span data-qa="username">{user.weakLoginUsername}</span>
                  <Gap horizontal size="xs" />
                  <InfoButton
                    onClick={toggleUsernameInfo}
                    aria-label={i18n.common.openExpandingInfo}
                  />
                </div>
                {usernameInfo && (
                  <FullRow>
                    <ExpandingInfoBox
                      info={t.usernameInfo}
                      close={closeUsernameInfo}
                    />
                  </FullRow>
                )}
                <Label>{t.password}</Label>
                <div>
                  <div>********</div>
                  <Button
                    data-qa="update-password"
                    appearance="inline"
                    text={t.updatePassword}
                    icon={canEdit ? undefined : faLockAlt}
                    onClick={canEdit ? openModal : navigateToLogin}
                  />
                </div>
              </>
            ) : (
              <FullRow>
                {!isEmailVerified && (
                  <AlertBox message={t.unverifiedEmailWarning} />
                )}
                <Button
                  data-qa="activate-credentials"
                  disabled={!isEmailVerified}
                  text={t.activateCredentials}
                  icon={canEdit ? undefined : faLockAlt}
                  onClick={canEdit ? openModal : navigateToLogin}
                />
              </FullRow>
            )}
          </>
        )}
      </Grid>
      <ModalAccessibilityWrapper>
        {!!emailVerificationStatus.verifiedEmail && (
          <>
            {modalOpen && (
              <WeakCredentialsFormModal
                hasCredentials={!!user.weakLoginUsername}
                username={
                  user.weakLoginUsername ??
                  emailVerificationStatus.verifiedEmail
                }
                onSuccess={() => {
                  closeModal()
                  reloadUser()
                  if (!user.weakLoginUsername) {
                    openActivationSuccessModal()
                  }
                }}
                onCancel={closeModal}
              />
            )}
            {activationSuccessModalOpen && (
              <BaseModal
                data-qa="weak-credentials-modal"
                type="success"
                title={t.activationSuccess}
                icon={faCheck}
                close={closeActivationSuccessModal}
                closeLabel={i18n.common.close}
              >
                <ModalButtons $justifyContent="center">
                  <Button
                    data-qa="modal-okBtn"
                    primary
                    text={t.activationSuccessOk}
                    onClick={closeActivationSuccessModal}
                  />
                </ModalButtons>
              </BaseModal>
            )}
          </>
        )}
      </ModalAccessibilityWrapper>
    </div>
  )
})

const minLength = 8
const maxLength = 128

const passwordForm = validated(
  object({
    password: string(),
    confirmPassword: string()
  }),
  (form) => {
    if (
      form.password.length === 0 ||
      form.password !== form.confirmPassword ||
      form.password.length < minLength ||
      form.password.length > maxLength
    ) {
      return 'required'
    }
    return undefined
  }
)

const UsernameField = styled.input`
  cursor: auto;
  border: none;
`

const WeakCredentialsFormModal = React.memo(function WeakCredentialsFormModal({
  hasCredentials,
  username,
  onSuccess,
  onCancel
}: {
  hasCredentials: boolean
  username: string
  onSuccess: () => void
  onCancel: () => void
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.loginDetailsSection

  const form = useForm(
    passwordForm,
    () => ({ password: '', confirmPassword: '' }),
    i18n.validationErrors
  )
  const { password, confirmPassword } = useFormFields(form)
  const pattern = `.{${minLength},${maxLength}}`
  return (
    <MutateFormModal
      data-qa="weak-credentials-modal"
      title={t.weakLoginCredentials}
      resolveLabel={
        hasCredentials ? t.updatePassword : t.confirmActivateCredentials
      }
      rejectLabel={i18n.common.cancel}
      resolveMutation={updateWeakLoginCredentialsMutation}
      resolveAction={() => ({
        body: {
          username: hasCredentials ? null : username,
          password: form.value().password
        }
      })}
      rejectAction={onCancel}
      onSuccess={onSuccess}
      resolveDisabled={!form.isValid()}
    >
      <form onClick={(e) => e.preventDefault()}>
        <FixedSpaceColumn spacing="xs">
          <Label htmlFor="username">{t.weakLoginUsername}</Label>
          <UsernameField
            data-qa="username"
            id="username"
            name="username"
            type="email"
            autoComplete="email"
            readOnly
            value={username}
          />
          <Label htmlFor="password">{t.password}</Label>
          <InputFieldF
            data-qa="password"
            id="password"
            name="password"
            autoComplete="new-password"
            autoFocus={true}
            bind={password}
            type="password"
            width="full"
            hideErrorsBeforeTouched={true}
            pattern={pattern}
          />
          <Label htmlFor="confirm-password">{t.confirmPassword}</Label>
          <InputFieldF
            data-qa="confirm-password"
            id="confirm-password"
            name="confirm-password"
            autoComplete="new-password"
            bind={confirmPassword}
            type="password"
            width="full"
            hideErrorsBeforeTouched={true}
            pattern={pattern}
          />
        </FixedSpaceColumn>
      </form>
    </MutateFormModal>
  )
})
