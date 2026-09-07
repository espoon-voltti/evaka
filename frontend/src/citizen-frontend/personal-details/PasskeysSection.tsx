// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import type { CitizenPasskeyId } from 'lib-common/generated/api-types/shared'
import type {
  CitizenPasskey,
  DeviceClass
} from 'lib-common/generated/api-types/user'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { InformationText, Label, LabelLike, P } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import {
  faLaptop,
  faLockAlt,
  faMobileButton,
  faPen,
  faPlus,
  faTabletButton,
  faTrash
} from 'lib-icons'

import ModalAccessibilityWrapper from '../ModalAccessibilityWrapper'
import { renderResult } from '../async-rendering'
import { createPasskeyCredential, passkeysSupported } from '../auth/passkeys'
import type { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import { SectionTitle } from './components'
import {
  deletePasskeyMutation,
  finishPasskeyRegistrationMutation,
  passkeysQuery,
  updatePasskeyNameMutation
} from './queries'

export default React.memo(function PasskeysSection({ user }: { user: User }) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeysSection

  const canAdd = passkeysSupported()
  const canEdit = user.authLevel === 'STRONG'
  const passkeys = useQueryResult(passkeysQuery())

  const [editing, setEditing] = useState<CitizenPasskeyId | null>(null)
  const [passkeyToDelete, setPasskeyToDelete] = useState<CitizenPasskey | null>(
    null
  )
  const [addError, setAddError] = useState<
    'alreadyExists' | 'limit' | 'generic' | null
  >(null)

  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  const { mutateAsync: finishRegistration } = useMutationResult(
    finishPasskeyRegistrationMutation
  )

  const addPasskey = useCallback(async () => {
    setAddError(null)
    const created = await createPasskeyCredential()
    switch (created.status) {
      case 'alreadyExists':
        setAddError('alreadyExists')
        return
      case 'failure':
        setAddError(created.errorCode === 'PASSKEY_LIMIT' ? 'limit' : 'generic')
        return
      case 'cancelled':
        return
    }

    const saved = await finishRegistration({
      body: {
        name: created.providerName ?? t.defaultName,
        credential: created.credential
      }
    })
    if (saved.isFailure) {
      setAddError(saved.errorCode === 'PASSKEY_LIMIT' ? 'limit' : 'generic')
    }
  }, [finishRegistration, t.defaultName])

  return (
    <div data-qa="passkeys-section">
      <SectionTitle>{t.title}</SectionTitle>
      <P>{t.description}</P>
      {renderResult(passkeys, (passkeys) => (
        <FixedSpaceColumn $spacing="s">
          {canAdd && (
            <div>
              <Button
                appearance="inline"
                icon={canEdit ? faPlus : faLockAlt}
                text={t.addPasskey}
                onClick={canEdit ? () => void addPasskey() : navigateToLogin}
                data-qa="add-passkey"
              />
            </div>
          )}
          {addError !== null && (
            <AlertBox
              message={
                addError === 'alreadyExists'
                  ? t.alreadyExistsError
                  : addError === 'limit'
                    ? t.limitError
                    : t.addError
              }
              data-qa="add-passkey-error"
            />
          )}
          <FixedSpaceColumn $spacing="xs">
            {passkeys.map((passkey) =>
              passkey.id === editing ? (
                <PasskeyNameEditor
                  key={passkey.id}
                  passkey={passkey}
                  onClose={() => setEditing(null)}
                />
              ) : (
                <PasskeyRow
                  key={passkey.id}
                  passkey={passkey}
                  canEdit={canEdit}
                  onStartEditing={() => setEditing(passkey.id)}
                  onDelete={() => setPasskeyToDelete(passkey)}
                  navigateToLogin={navigateToLogin}
                />
              )
            )}
          </FixedSpaceColumn>
        </FixedSpaceColumn>
      ))}
      <ModalAccessibilityWrapper>
        {passkeyToDelete !== null && (
          <MutateFormModal
            data-qa="delete-passkey-modal"
            type="danger"
            title={t.deleteConfirmTitle}
            text={t.deleteConfirmText(passkeyToDelete.name)}
            icon={faTrash}
            resolveLabel={t.deletePasskey}
            resolveDanger
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

const PasskeyRow = React.memo(function PasskeyRow({
  passkey,
  canEdit,
  onStartEditing,
  onDelete,
  navigateToLogin
}: {
  passkey: CitizenPasskey
  canEdit: boolean
  onStartEditing: () => void
  onDelete: () => void
  navigateToLogin: () => void
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeysSection
  return (
    <PasskeyCard deviceClass={passkey.deviceClass}>
      <FixedSpaceColumn $spacing="xxs">
        <NameRow>
          <PasskeyName data-qa="passkey-name">{passkey.name}</PasskeyName>
          {canEdit ? (
            <>
              <IconOnlyButton
                icon={faPen}
                aria-label={t.editName}
                onClick={onStartEditing}
                data-qa="edit-passkey"
              />
              <IconOnlyButton
                icon={faTrash}
                aria-label={t.deletePasskey}
                onClick={onDelete}
                data-qa="delete-passkey"
              />
            </>
          ) : (
            <Button
              appearance="inline"
              icon={faLockAlt}
              text={i18n.common.edit}
              onClick={navigateToLogin}
              data-qa="edit-passkey-login"
            />
          )}
        </NameRow>
        <PasskeyDetails passkey={passkey} />
      </FixedSpaceColumn>
    </PasskeyCard>
  )
})

const passkeyNameForm = object({
  name: validated(required(string()), nonBlank)
})

const PasskeyNameEditor = React.memo(function PasskeyNameEditor({
  passkey,
  onClose
}: {
  passkey: CitizenPasskey
  onClose: () => void
}) {
  const i18n = useTranslation()
  const t = i18n.personalDetails.passkeysSection

  const form = useForm(
    passkeyNameForm,
    () => ({ name: passkey.name }),
    i18n.validationErrors
  )
  const { name } = useFormFields(form)
  const inputId = `passkey-name-${passkey.id}`

  return (
    <PasskeyCard deviceClass={passkey.deviceClass}>
      <FixedSpaceColumn $spacing="s">
        <FixedSpaceColumn $spacing="xs">
          <Label htmlFor={inputId}>{t.nameLabel}</Label>
          <InputFieldF
            id={inputId}
            data-qa="passkey-name-input"
            bind={name}
            width="full"
            autoFocus={true}
            hideErrorsBeforeTouched={true}
          />
        </FixedSpaceColumn>
        <PasskeyDetails passkey={passkey} />
        <EditActions>
          <Button
            appearance="inline"
            text={i18n.common.cancel}
            onClick={onClose}
            data-qa="cancel-passkey-name"
          />
          <MutateButton
            primary
            text={i18n.common.save}
            mutation={updatePasskeyNameMutation}
            onClick={() => ({
              id: passkey.id,
              body: { name: form.value().name }
            })}
            onSuccess={onClose}
            disabled={!form.isValid()}
            data-qa="save-passkey-name"
          />
        </EditActions>
      </FixedSpaceColumn>
    </PasskeyCard>
  )
})

const PasskeyDetails = React.memo(function PasskeyDetails({
  passkey
}: {
  passkey: CitizenPasskey
}) {
  const t = useTranslation().personalDetails.passkeysSection
  const created = [
    passkey.agentName,
    passkey.operatingSystemName,
    passkey.createdAt.toLocalDate().format()
  ].filter((part) => part !== '')
  return (
    <DetailLines>
      <InformationText data-qa="passkey-created">
        {t.added}: {created.join(', ')}
      </InformationText>
      <InformationText data-qa="passkey-last-used">
        {t.lastUsed}: {passkey.lastUsedAt?.format() ?? t.neverUsed}
      </InformationText>
    </DetailLines>
  )
})

const deviceIcon = (deviceClass: DeviceClass) => {
  switch (deviceClass) {
    case 'PHONE':
      return faMobileButton
    case 'TABLET':
      return faTabletButton
    case 'DESKTOP':
    case 'UNKNOWN':
      return faLaptop
  }
}

const PasskeyCard = React.memo(function PasskeyCard({
  deviceClass,
  children
}: {
  deviceClass: DeviceClass
  children: React.ReactNode
}) {
  return (
    <CardFrame data-qa="passkey">
      <CardIcon>
        <FontAwesomeIcon icon={deviceIcon(deviceClass)} />
      </CardIcon>
      <CardContent>{children}</CardContent>
    </CardFrame>
  )
})

const CardFrame = styled.div`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
  border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  border-radius: 4px;
  padding: ${defaultMargins.s};
`

const CardIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  font-size: 20px;
  color: ${(p) => p.theme.colors.grayscale.g100};
`

const CardContent = styled.div`
  flex: 1 0 0;
  min-width: 0;
`

const NameRow = styled.div`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.m};
`

const PasskeyName = styled(LabelLike)`
  flex: 1 0 0;
  min-width: 0;
  word-break: break-word;
`

const DetailLines = styled.div`
  display: flex;
  flex-direction: column;
  line-height: 24px;
`

const EditActions = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: ${defaultMargins.s};
`
