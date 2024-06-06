// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import { combine, Failure, Result, Success } from 'lib-common/api'
import { boolean, string } from 'lib-common/form/fields'
import { array, mapped, object, value } from 'lib-common/form/form'
import {
  BoundForm,
  useBoolean,
  useForm,
  useFormElems,
  useFormField
} from 'lib-common/form/hooks'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import {
  pushNotificationCategories,
  PushNotificationCategory,
  PushSettings
} from 'lib-common/generated/api-types/webpush'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AsyncInlineButton from 'lib-components/atoms/buttons/AsyncInlineButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import Checkbox, { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { fontWeights, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { ServiceWorkerContext } from '../common/service-worker'
import { unitInfoQuery } from '../units/queries'

import { pushSettingsMutation, pushSettingsQuery } from './queries'

const SectionLabel = styled.div`
  font-weight: ${fontWeights.semibold};
`

const EditButton = styled(InlineButton)`
  font-size: 16px;
`

export const NotificationSettings = React.memo(function NotificationSettings({
  unitId
}: {
  unitId: UUID
}) {
  const { i18n } = useTranslation()
  const t = i18n.settings.notifications
  const { pushNotifications } = useContext(ServiceWorkerContext)
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))
  const [permissionState, setPermissionState] = useState<
    PermissionState | undefined
  >(undefined)

  const refreshPermissionState = useCallback(() => {
    pushNotifications?.permissionState
      .then(setPermissionState)
      .catch(() => setPermissionState(undefined))
  }, [pushNotifications])
  useEffect(refreshPermissionState, [refreshPermissionState])

  const pushSettingsResult: Result<PushSettings> = useQueryResult(
    pushSettingsQuery(),
    {
      enabled: permissionState === 'granted'
    }
  )
  const groupInfosResponse = useMemo(
    () => unitInfoResponse.map(({ groups }) => sortBy(groups, (g) => g.name)),
    [unitInfoResponse]
  )

  const [editing, { on: startEditing, off: stopEditing }] = useBoolean(false)

  const enable = useCallback(
    () =>
      pushNotifications
        ?.enable()
        .then((success) =>
          success ? Success.of(undefined) : Failure.of({ message: 'denied' })
        )
        .catch((err) => Failure.fromError(err)),
    [pushNotifications]
  )
  return (
    <div data-qa="notification-settings">
      <H2>
        {t.title}
        {permissionState === 'granted' && !editing ? (
          <>
            <Gap size="s" horizontal />
            <EditButton
              data-qa="edit"
              text={i18n.common.edit}
              onClick={startEditing}
            />
          </>
        ) : undefined}
      </H2>
      <div>
        <SectionLabel>{t.permission.label}</SectionLabel>
        <Gap size="s" />
        <PermissionSection
          state={pushNotifications ? permissionState : 'unsupported'}
          refresh={refreshPermissionState}
          enable={enable}
        />
      </div>
      {permissionState === 'granted' &&
        renderResult(
          combine(pushSettingsResult, groupInfosResponse),
          ([pushSettings, groupInfos]) => (
            <>
              <Gap size="m" />
              {editing ? (
                <SettingsSectionsEditor
                  stopEditing={stopEditing}
                  pushSettings={pushSettings}
                  groupInfos={groupInfos}
                />
              ) : (
                <SettingsSectionsView
                  pushSettings={pushSettings}
                  groupInfos={groupInfos}
                />
              )}
            </>
          )
        )}
    </div>
  )
})

const SettingsSections = React.memo(function SettingsSections(props: {
  categories: React.ReactNode
  groups: React.ReactNode
}) {
  const { i18n } = useTranslation()
  const t = i18n.settings.notifications

  return (
    <>
      <div data-qa="categories">
        <SectionLabel>{t.categories.label}</SectionLabel>
        <Gap size="s" />
        {props.categories}
      </div>
      <Gap size="m" />
      <div data-qa="groups">
        <SectionLabel>{t.groups.label}</SectionLabel>
        <Gap size="s" />
        {props.groups}
      </div>
    </>
  )
})

const SettingsSectionsView = React.memo(function SettingsSectionsView({
  pushSettings,
  groupInfos
}: {
  pushSettings: PushSettings
  groupInfos: GroupInfo[]
}) {
  const { i18n } = useTranslation()
  return (
    <SettingsSections
      categories={pushNotificationCategories.map((category) => (
        <Checkbox
          key={category}
          data-qa={category}
          label={i18n.settings.notifications.categories.values[category]}
          checked={pushSettings.categories.includes(category)}
          disabled={true}
        />
      ))}
      groups={groupInfos.map((group) => (
        <Checkbox
          key={group.id}
          data-qa={group.id}
          label={group.name}
          checked={pushSettings.groups.includes(group.id)}
          disabled={true}
        />
      ))}
    />
  )
})

const categoryForm = object({
  category: value<PushNotificationCategory>(),
  checked: boolean()
})

const groupForm = object({
  id: value<UUID>(),
  name: string(),
  checked: boolean()
})

const settingsForm = mapped(
  object({
    categories: array(categoryForm),
    groups: array(groupForm),
    // On personal mobile devices, we might be subscribed to groups that belong to a unit that isn't selected, so
    // they can't appear as checkboxes
    hiddenGroups: array(value<UUID>())
  }),
  ({ categories, groups, hiddenGroups }) => ({
    categories: categories
      .filter(({ checked }) => checked)
      .map(({ category }) => category),
    groups: [
      ...hiddenGroups,
      ...groups.filter(({ checked }) => checked).map(({ id }) => id)
    ]
  })
)

const SettingsSectionsEditor = React.memo(function SettingsSectionsEditor({
  stopEditing,
  pushSettings,
  groupInfos
}: {
  stopEditing: () => void
  pushSettings: PushSettings
  groupInfos: GroupInfo[]
}) {
  const { i18n } = useTranslation()
  const form = useForm(
    settingsForm,
    () => ({
      categories: pushNotificationCategories.map((category) => ({
        category,
        checked: pushSettings.categories.includes(category)
      })),
      groups: groupInfos.map((group) => ({
        id: group.id,
        name: group.name,
        checked: pushSettings.groups.includes(group.id)
      })),
      hiddenGroups: pushSettings.groups.filter((group) =>
        groupInfos.every(({ id }) => group !== id)
      )
    }),
    {}
  )

  const categories = useFormElems(useFormField(form, 'categories'))
  const groups = useFormElems(useFormField(form, 'groups'))

  return (
    <>
      <SettingsSections
        categories={categories.map((f, idx) => (
          <CategoryCheckbox key={idx} form={f} />
        ))}
        groups={groups.map((f, idx) => (
          <GroupCheckbox key={idx} form={f} />
        ))}
      />
      <Gap size="L" />
      <FixedSpaceRow>
        <Button
          data-qa="cancel"
          onClick={stopEditing}
          text={i18n.common.cancel}
        />
        <MutateButton
          primary
          data-qa="save"
          type="submit"
          text={i18n.common.save}
          mutation={pushSettingsMutation}
          onClick={() => ({
            body: form.value()
          })}
          onSuccess={stopEditing}
        />
      </FixedSpaceRow>
    </>
  )
})

const CategoryCheckbox = React.memo(function CategoryCheckbox({
  form
}: {
  form: BoundForm<typeof categoryForm>
}) {
  const { i18n } = useTranslation()
  const checked = useFormField(form, 'checked')

  return (
    <CheckboxF
      data-qa={form.state.category}
      bind={checked}
      label={i18n.settings.notifications.categories.values[form.state.category]}
    />
  )
})

const GroupCheckbox = React.memo(function CategoryCheckbox({
  form
}: {
  form: BoundForm<typeof groupForm>
}) {
  const checked = useFormField(form, 'checked')

  return (
    <CheckboxF data-qa={form.state.id} bind={checked} label={form.state.name} />
  )
})

const PermissionSection = React.memo(function PermissionSection(props: {
  state: PermissionState | 'unsupported' | undefined
  refresh: () => void
  enable: () => void
}) {
  const { i18n } = useTranslation()
  const t = i18n.settings.notifications.permission

  const [showInfo, { on: openInfo, off: closeInfo }] = useBoolean(false)

  switch (props.state) {
    case 'unsupported':
      return (
        <>
          <div>
            <span data-qa="permission-state">{t.state.unsupported}</span>
            <InlineInfoButton
              onClick={openInfo}
              aria-label={i18n.common.openExpandingInfo}
            />
          </div>
          {showInfo && (
            <ExpandingInfoBox info={t.info.unsupported} close={closeInfo} />
          )}
        </>
      )
    case 'prompt':
      return (
        <FixedSpaceRow>
          <span data-qa="permission-state">{t.state.prompt}</span>
          <Gap size="s" horizontal />
          <AsyncInlineButton
            data-qa="enable"
            text={t.enable}
            onClick={props.enable}
            onFailure={props.refresh}
            onSuccess={props.refresh}
          />
        </FixedSpaceRow>
      )
    case 'granted':
      return <div data-qa="permission-state">{t.state.granted}</div>
    case 'denied':
      return (
        <>
          <div>
            <span data-qa="permission-state">{t.state.denied}</span>
            <InlineInfoButton
              onClick={openInfo}
              aria-label={i18n.common.openExpandingInfo}
            />
          </div>
          {showInfo && (
            <ExpandingInfoBox info={t.info.denied} close={closeInfo} />
          )}
        </>
      )
    default:
      return null
  }
})
