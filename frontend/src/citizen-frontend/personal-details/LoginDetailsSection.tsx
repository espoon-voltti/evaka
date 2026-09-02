// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Failure } from 'lib-common/api'
import { object, required, validated, value } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'
import type { PasswordConstraints } from 'lib-common/generated/api-types/shared'
import { isPasswordStructureValid } from 'lib-common/password'
import { useQueryResult } from 'lib-common/query'
import { Chip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  MobileFixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import PasswordInputF from 'lib-components/molecules/PasswordInputF'
import BaseModal, {
  ModalButtons
} from 'lib-components/molecules/modals/BaseModal'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label, LabelLike, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faCheck, faLockAlt, faTrash } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import type { User } from '../auth/state'
import { useTranslation } from '../localization'
import { forgetLastLoginMethod } from '../login/last-login-method'
import { getStrongLoginUri } from '../navigation/const'

import {
  DataRow,
  DataRowLabel,
  DataRowValue,
  SectionTitle,
  TitleAndEditRow
} from './components'
import { isEmailVerified } from './emailVerification'
import {
  deleteWeakLoginCredentialsMutation,
  passkeysQuery,
  updateWeakLoginCredentialsMutation
} from './queries'

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

  const emailVerified = isEmailVerified(emailVerificationStatus)

  const passkeys = useQueryResult(passkeysQuery())
  const noPasskeys = passkeys.map((p) => p.length === 0).getOrElse(null)

  const [modalOpen, { off: closeModal, on: openModal }] = useBoolean(false)
  const [disableModalOpen, { off: closeDisableModal, on: openDisableModal }] =
    useBoolean(false)
  const [
    activationSuccessModalOpen,
    { off: closeActivationSuccessModal, on: openActivationSuccessModal }
  ] = useBoolean(false)

  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  return (
    <div data-qa="login-details-section">
      <TitleAndEditRow>
        <SectionTitle $noMargin>{t.title}</SectionTitle>
      </TitleAndEditRow>

      <Gap $size="xs" />

      {user.weakLoginUsername ? (
        <>
          <Chip
            colorPalette="green"
            icon={faCheck}
            label={t.status.enabled}
            iconCircle
            size="small"
            data-qa="weak-login-enabled"
          />
          <Gap $size="xs" />
          <DataRow>
            <DataRowLabel>{t.weakLoginUsername}</DataRowLabel>
            <DataRowValue>
              <span data-qa="username" translate="no">
                {user.weakLoginUsername}
              </span>
            </DataRowValue>
          </DataRow>
          <DataRow>
            <DataRowLabel>{t.password}</DataRowLabel>
            <DataRowValue>********</DataRowValue>
          </DataRow>
          <Gap $size="s" />
          <MobileFixedSpaceRow $spacing="L">
            <Button
              appearance="inline"
              data-qa="update-password"
              text={t.updatePassword}
              icon={canEdit ? undefined : faLockAlt}
              onClick={canEdit ? openModal : navigateToLogin}
            />
            <Button
              appearance="inline"
              data-qa="disable-credentials"
              text={t.disableCredentials}
              icon={canEdit ? undefined : faLockAlt}
              onClick={canEdit ? openDisableModal : navigateToLogin}
            />
          </MobileFixedSpaceRow>
        </>
      ) : emailVerified ? (
        <Button
          data-qa="activate-credentials"
          text={t.activateCredentials}
          icon={canEdit ? undefined : faLockAlt}
          onClick={canEdit ? openModal : navigateToLogin}
        />
      ) : (
        <div data-qa="weak-login-disabled">{t.unverifiedEmailWarning}</div>
      )}
      <ModalAccessibilityWrapper>
        {disableModalOpen && (
          <MutateFormModal
            data-qa="disable-credentials-modal"
            type="danger"
            title={t.disableConfirmTitle}
            text={
              <>
                <P $noMargin>{t.disableConfirmText}</P>
                {noPasskeys !== null && (
                  <>
                    <Gap $size="s" />
                    <P
                      $noMargin
                      data-qa={
                        noPasskeys ? 'no-passkeys-warning' : 'has-passkeys-info'
                      }
                    >
                      {noPasskeys
                        ? t.disableConfirmNoPasskeys
                        : t.disableConfirmHasPasskeys}
                    </P>
                  </>
                )}
                <Gap $size="s" />
                <P $noMargin>{t.disableConfirmReactivate}</P>
              </>
            }
            icon={faTrash}
            resolveLabel={t.disableCredentials}
            resolveDanger
            rejectLabel={i18n.common.cancel}
            resolveMutation={deleteWeakLoginCredentialsMutation}
            resolveAction={() => undefined}
            rejectAction={closeDisableModal}
            onSuccess={() => {
              closeDisableModal()
              forgetLastLoginMethod('email')
              reloadUser()
            }}
          />
        )}
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
          password: form.value().password
        }
      })}
      rejectAction={onCancel}
      onFailure={onFailure}
      onSuccess={onSuccess}
      resolveDisabled={!form.isValid() || isUnacceptable || isUsernameConflict}
    >
      <form onClick={(e) => e.preventDefault()}>
        <FixedSpaceColumn $spacing="xs">
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
          <PasswordInputF
            data-qa="password"
            id="password"
            name="password"
            autoComplete="new-password"
            autoFocus={true}
            bind={password}
            width="full"
            hideErrorsBeforeTouched={true}
            pattern={pattern}
          />
          <Label htmlFor="confirm-password">{t.confirmPassword}</Label>
          <PasswordInputF
            data-qa="confirm-password"
            id="confirm-password"
            name="confirm-password"
            autoComplete="new-password"
            bind={confirmPassword}
            width="full"
            hideErrorsBeforeTouched={true}
            pattern={pattern}
          />
          <Gap $size="xs" />
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
