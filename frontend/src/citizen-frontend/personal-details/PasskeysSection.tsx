// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { string } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import type { CitizenPasskey } from 'lib-common/generated/api-types/user'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { H2, Label, LabelLike, P } from 'lib-components/typography'
import { faLockAlt, faTrash } from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { renderResult } from '../async-rendering'
import { createPasskeyCredential } from '../auth/passkeys'
import type { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import {
  deletePasskeyMutation,
  finishPasskeyRegistrationMutation,
  passkeysQuery
} from './queries'

export default React.memo(function PasskeysSection({ user }: { user: User }) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeysSection

  const canEdit = user.authLevel === 'STRONG'
  const passkeys = useQueryResult(passkeysQuery())

  const [pendingCredential, setPendingCredential] = useState<{
    credential: string
    defaultName: string
  } | null>(null)
  const [passkeyToDelete, setPasskeyToDelete] = useState<CitizenPasskey | null>(
    null
  )
  const [addError, setAddError] = useState<'limit' | 'generic' | null>(null)

  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  const startAdd = useCallback(async () => {
    setAddError(null)
    const result = await createPasskeyCredential()
    if (result.status === 'success') {
      setPendingCredential({
        credential: result.credential,
        defaultName: result.providerName ?? t.defaultName
      })
    } else if (result.status === 'failure') {
      setAddError(result.errorCode === 'PASSKEY_LIMIT' ? 'limit' : 'generic')
    }
  }, [t.defaultName])

  return (
    <div data-qa="passkeys-section">
      <H2>{t.title}</H2>
      <P>{t.description}</P>
      {renderResult(passkeys, (passkeys) => (
        <FixedSpaceColumn $spacing="m">
          {passkeys.map((passkey) => (
            <FixedSpaceColumn key={passkey.id} $spacing="xxs" data-qa="passkey">
              <LabelLike data-qa="passkey-name">{passkey.name}</LabelLike>
              <div>
                {t.added}: {passkey.createdAt.toLocalDate().format()}
              </div>
              <div data-qa="passkey-last-used">
                {t.lastUsed}: {passkey.lastUsedAt?.format() ?? t.neverUsed}
              </div>
              <div>
                <Button
                  appearance="inline"
                  icon={canEdit ? faTrash : faLockAlt}
                  text={t.deletePasskey}
                  onClick={
                    canEdit
                      ? () => setPasskeyToDelete(passkey)
                      : navigateToLogin
                  }
                  data-qa="delete-passkey"
                />
              </div>
            </FixedSpaceColumn>
          ))}
          {addError !== null && (
            <AlertBox
              message={addError === 'limit' ? t.limitError : t.addError}
              noMargin
              data-qa="add-passkey-error"
            />
          )}
          <FixedSpaceRow>
            <Button
              text={t.addPasskey}
              icon={canEdit ? undefined : faLockAlt}
              onClick={canEdit ? () => void startAdd() : navigateToLogin}
              data-qa="add-passkey"
            />
          </FixedSpaceRow>
        </FixedSpaceColumn>
      ))}
      <ModalAccessibilityWrapper>
        {pendingCredential !== null && (
          <PasskeyNameModal
            credential={pendingCredential.credential}
            defaultName={pendingCredential.defaultName}
            onClose={() => setPendingCredential(null)}
            onFailure={(errorCode) => {
              setPendingCredential(null)
              setAddError(errorCode === 'PASSKEY_LIMIT' ? 'limit' : 'generic')
            }}
          />
        )}
        {passkeyToDelete !== null && (
          <MutateFormModal
            data-qa="delete-passkey-modal"
            type="warning"
            title={t.deleteConfirmTitle}
            text={t.deleteConfirmText(passkeyToDelete.name)}
            icon={faTrash}
            resolveLabel={t.deletePasskey}
            rejectLabel={i18n.common.cancel}
            resolveMutation={deletePasskeyMutation}
            resolveAction={() => ({ id: passkeyToDelete.id })}
            rejectAction={() => setPasskeyToDelete(null)}
            onSuccess={() => setPasskeyToDelete(null)}
          />
        )}
      </ModalAccessibilityWrapper>
    </div>
  )
})

const passkeyNameForm = object({
  name: validated(required(string()), nonBlank)
})

const PasskeyNameModal = React.memo(function PasskeyNameModal({
  credential,
  defaultName,
  onClose,
  onFailure
}: {
  credential: string
  defaultName: string
  onClose: () => void
  onFailure: (errorCode: string | undefined) => void
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeysSection

  const form = useForm(
    passkeyNameForm,
    () => ({ name: defaultName }),
    i18n.validationErrors
  )
  const { name } = useFormFields(form)

  return (
    <MutateFormModal
      data-qa="passkey-name-modal"
      title={t.nameModalTitle}
      resolveLabel={i18n.common.save}
      rejectLabel={i18n.common.cancel}
      resolveMutation={finishPasskeyRegistrationMutation}
      resolveAction={() => ({
        body: { name: form.value().name, credential }
      })}
      rejectAction={onClose}
      onSuccess={onClose}
      onFailure={(failure) => onFailure(failure.errorCode)}
      resolveDisabled={!form.isValid()}
    >
      <FixedSpaceColumn $spacing="xs">
        <Label htmlFor="passkey-name">{t.nameLabel}</Label>
        <InputFieldF
          id="passkey-name"
          data-qa="passkey-name-input"
          bind={name}
          width="full"
          autoFocus={true}
          hideErrorsBeforeTouched={true}
        />
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})
