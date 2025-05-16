// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { Failure } from 'lib-common/api'
import { object, required, validated, value } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import { PasswordConstraints } from 'lib-common/generated/api-types/shared'
import { isPasswordStructureValid } from 'lib-common/password'
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
import { H2, Label, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faCheck, faLockAlt } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { FullRow, Grid } from './components'
import { updateWeakLoginCredentialsMutation } from './queries'

export interface Props {
  user: User
  reloadUser: () => void
  emailVerificationStatus: EmailVerificationStatusResponse
  passwordConstraints: PasswordConstraints
}

export default React.memo(function LoginDetailsSection({
  user,
  reloadUser,
  emailVerificationStatus,
  passwordConstraints
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
        {(!!user.email || !!user.weakLoginUsername) && (
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
                passwordConstraints={passwordConstraints}
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

const UsernameField = styled.input`
  cursor: auto;
  border: none;
`

const ConstraintsList = styled.ul`
  margin: 0;
`

const WeakCredentialsFormModal = React.memo(function WeakCredentialsFormModal({
  passwordConstraints,
  hasCredentials,
  username,
  onSuccess,
  onCancel
}: {
  passwordConstraints: PasswordConstraints
  hasCredentials: boolean
  username: string
  onSuccess: () => void
  onCancel: () => void
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.loginDetailsSection

  const passwordForm = useMemo(
    () =>
      validated(
        object({
          // value<string> is used to avoid trimming
          password: validated(required(value<string>()), (password) =>
            isPasswordStructureValid(passwordConstraints, password)
              ? undefined
              : 'passwordFormat'
          ),
          confirmPassword: required(value<string>())
        }),
        (form) =>
          form.password !== form.confirmPassword
            ? { confirmPassword: 'passwordMismatch' }
            : undefined
      ),
    [passwordConstraints]
  )

  const [isUnacceptable, setUnacceptable] = useState<boolean>(false)
  const [isUsernameConflict, setUsernameConflict] = useState<boolean>(false)

  const form = useForm(
    passwordForm,
    () => ({ password: '', confirmPassword: '' }),
    {
      ...i18n.validationErrors,
      ...t.validationErrors
    },
    {
      // clear error when password is updated
      onUpdate: (prev, next, _) => {
        if (prev.password !== next.password) {
          setUnacceptable(false)
        }
        return next
      }
    }
  )
  const { password, confirmPassword } = useFormFields(form)
  const pattern = `.{${passwordConstraints.minLength},${passwordConstraints.maxLength}}`

  const onFailure = useCallback(
    (failure: Failure<unknown>) => {
      setUnacceptable(failure.errorCode === 'PASSWORD_UNACCEPTABLE')
      setUsernameConflict(failure.statusCode === 409)
    },
    [setUnacceptable]
  )

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
      onFailure={onFailure}
      onSuccess={onSuccess}
      resolveDisabled={!form.isValid() || isUnacceptable || isUsernameConflict}
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
          <Gap size="xs" />
          <LabelLike>{`${t.passwordConstraints.label}:`}</LabelLike>
          <ConstraintsList>
            <li>
              {t.passwordConstraints.length(
                passwordConstraints.minLength,
                passwordConstraints.maxLength
              )}
            </li>
            {passwordConstraints.minLowers > 0 && (
              <li>
                {t.passwordConstraints.minLowers(passwordConstraints.minLowers)}
              </li>
            )}
            {passwordConstraints.minUppers > 0 && (
              <li>
                {t.passwordConstraints.minUppers(passwordConstraints.minUppers)}
              </li>
            )}
            {passwordConstraints.minDigits > 0 && (
              <li>
                {t.passwordConstraints.minDigits(passwordConstraints.minDigits)}
              </li>
            )}
            {passwordConstraints.minSymbols > 0 && (
              <li>
                {t.passwordConstraints.minSymbols(
                  passwordConstraints.minSymbols
                )}
              </li>
            )}
          </ConstraintsList>
          {isUnacceptable && (
            <AlertBox
              data-qa="unacceptable-password-alert"
              message={t.unacceptablePassword}
            />
          )}
          {isUsernameConflict && (
            <AlertBox message={t.usernameConflict(username)} />
          )}
        </FixedSpaceColumn>
      </form>
    </MutateFormModal>
  )
})
