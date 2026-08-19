// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { object, oneOf, required } from 'lib-common/form/form'
import { useBoolean, useForm, useFormField } from 'lib-common/form/hooks'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { InformationText } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import type { User } from '../auth/state'
import { useTranslation } from '../localization'
import { getStrongLoginUri } from '../navigation/const'

import {
  DataRow,
  DataRowLabel,
  DataRowValue,
  EditableSectionHeader
} from './components'
import { updatePersonalDetailsMutation } from './queries'

interface Props {
  user: User
  reloadUser: () => void
}

const personDetailsForm = object({
  preferredName: required(oneOf<string>())
})

export default React.memo(function PersonalDetailsSection({
  user,
  reloadUser
}: Props) {
  const t = useTranslation()

  const [editMode, setEditMode] = useBoolean(false)

  const canEdit = user.authLevel === 'STRONG'
  const navigateToLogin = useCallback(
    () => window.location.replace(getStrongLoginUri()),
    []
  )

  const firstNames = user.firstName.split(' ')

  const initialFormState = {
    preferredName: {
      options: firstNames.map((name) => ({
        value: name,
        label: name,
        domValue: name
      })),
      domValue: user.preferredName || (firstNames[0] ?? '')
    }
  }

  const form = useForm(
    personDetailsForm,
    () => initialFormState,
    t.validationErrors
  )

  const preferredNameState = useFormField(form, 'preferredName')

  return (
    <div data-qa="person-details-section">
      <EditableSectionHeader
        title={t.personalDetails.detailsSection.title}
        editing={editMode}
        onStartEditing={setEditMode.on}
        onCancel={() => {
          setEditMode.off()
          form.set(initialFormState)
        }}
        mutation={updatePersonalDetailsMutation}
        onSave={() => ({
          body: {
            preferredName: form.value().preferredName,
            phone: null,
            backupPhone: null,
            email: null
          }
        })}
        onSaveSuccess={() => {
          reloadUser()
          setEditMode.off()
        }}
        saveDisabled={!form.isValid()}
        canEdit={canEdit}
        navigateToLogin={navigateToLogin}
      />
      {editMode && (
        <>
          <Gap $size="xs" />
          <InformationText>{t.personalDetails.editInfo}</InformationText>
          <Gap $size="s" />
        </>
      )}
      <DataRow>
        <DataRowLabel>{t.personalDetails.detailsSection.name}</DataRowLabel>
        <DataRowValue>
          <PersonName person={user} format="First Last" />
        </DataRowValue>
      </DataRow>
      <DataRow>
        <DataRowLabel htmlFor="preferred-name-input">
          {t.personalDetails.detailsSection.preferredName}
        </DataRowLabel>
        <DataRowValue>
          {editMode ? (
            <SelectF
              id="preferred-name-input"
              bind={preferredNameState}
              data-qa="preferred-name"
              autoFocus={true}
            />
          ) : (
            <span data-qa="preferred-name" translate="no">
              {user.preferredName}
            </span>
          )}
        </DataRowValue>
      </DataRow>
      <DataRow>
        <DataRowLabel>{t.personalDetails.detailsSection.address}</DataRowLabel>
        <DataRowValue>
          {user.streetAddress &&
            `${user.streetAddress}, ${user.postalCode} ${user.postOffice}`}
        </DataRowValue>
      </DataRow>
    </div>
  )
})
