// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { string } from 'lib-common/form/fields'
import { object, validated } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/citizen'

import { User } from '../auth/state'
import { useTranslation } from '../localization'

import { updatePasswordMutation } from './queries'

const minLength = 8
const maxLength = 128

export interface Props {
  user: User
  reloadUser: () => void
}

export default React.memo(function LoginDetailsSection({
  user,
  reloadUser
}: Props) {
  const t = useTranslation().personalDetails.loginDetailsSection
  return (
    <div data-qa="login-details-section">
      <ListGrid rowGap="s" columnGap="L" labelWidth="max-content">
        <H2 noMargin>{t.title}</H2>
        <div />
        <Label>
          {t.weakLoginUsername}
          {featureFlags.weakLogin ? ' (Keycloak)' : ''}
        </Label>
        <div data-qa="keycloak-email" translate="no">
          {user.keycloakEmail}
        </div>
        {featureFlags.weakLogin && !!user.weakLoginUsername && (
          <>
            <Label>
              {t.weakLoginUsername}
              {' (eVaka)'}
            </Label>
            <div data-qa="weak-login-username" translate="no">
              {user.weakLoginUsername}
            </div>
          </>
        )}
        {featureFlags.weakLogin &&
          user.authLevel === 'STRONG' &&
          !!user.weakLoginUsername && (
            <>
              <Label>{t.password}</Label>
              <PasswordSection user={user} reloadUser={reloadUser} />
            </>
          )}
      </ListGrid>
    </div>
  )
})

const passwordForm = validated(
  object({
    password1: string(),
    password2: string()
  }),
  (form) => {
    if (
      form.password1.length === 0 ||
      form.password1 !== form.password2 ||
      form.password1.length < minLength ||
      form.password1.length > maxLength
    ) {
      return 'required'
    }
    return undefined
  }
)

const PasswordForm = React.memo(function PasswordForm({
  onClose,
  reloadUser
}: {
  reloadUser: () => void
  onClose: () => void
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.loginDetailsSection

  const form = useForm(
    passwordForm,
    () => ({ password1: '', password2: '' }),
    i18n.validationErrors
  )
  const { password1, password2 } = useFormFields(form)
  const pattern = `.{${minLength},${maxLength}}`
  return (
    <form action="" onClick={(e) => e.preventDefault()}>
      <FixedSpaceColumn spacing="xs">
        <InputFieldF
          autoComplete="new-password"
          autoFocus={true}
          bind={password1}
          type="password"
          placeholder={t.newPassword}
          width="L"
          hideErrorsBeforeTouched={true}
          pattern={pattern}
        />
        <InputFieldF
          autoComplete="new-password"
          bind={password2}
          type="password"
          placeholder={t.repeatPassword}
          width="L"
          hideErrorsBeforeTouched={true}
          pattern={pattern}
        />
        <MutateButton
          type="submit"
          disabled={!form.isValid()}
          mutation={updatePasswordMutation}
          text={i18n.common.save}
          onClick={() => ({
            body: {
              password: form.state.password1
            }
          })}
          onSuccess={() => {
            reloadUser()
            onClose()
          }}
        />
      </FixedSpaceColumn>
    </form>
  )
})

const PasswordSection = React.memo(function PasswordSection({
  user,
  reloadUser
}: Props) {
  const t = useTranslation().personalDetails.loginDetailsSection

  const isWeakLoginSetup = !!user.weakLoginUsername
  const [editing, { on: startEditing, off: stopEditing }] = useBoolean(false)
  return (
    <div data-qa="password">
      {editing ? (
        <PasswordForm onClose={stopEditing} reloadUser={reloadUser} />
      ) : (
        <Button
          text={isWeakLoginSetup ? t.updatePassword : t.setPassword}
          appearance="link"
          onClick={startEditing}
        />
      )}
    </div>
  )
})
