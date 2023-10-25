// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'

import RequireAuth from 'citizen-frontend/RequireAuth'
import { useUser } from 'citizen-frontend/auth/state'
import ResponsiveWholePageCollapsible from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import {
  ChildConsentType,
  childConsentTypes
} from 'lib-common/generated/api-types/children'
import { useQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import Radio from 'lib-components/atoms/form/Radio'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, LabelLike, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faLockAlt } from 'lib-icons'

import { useTranslation } from '../../../localization'

import {
  childConsentNotificationsQuery,
  childConsentsQuery,
  insertChildConsentsMutation
} from './queries'

interface ChildConsentsProps {
  childId: UUID
}

export default React.memo(function ChildConsentsSection({
  childId
}: ChildConsentsProps) {
  const t = useTranslation()
  const [open, setOpen] = useState(false)
  const user = useUser()
  const { data: unconsentedCounts } = useQuery(childConsentNotificationsQuery())
  const unconsentedCount =
    (unconsentedCounts && unconsentedCounts[childId]) ?? 0

  return (
    <ResponsiveWholePageCollapsible
      title={t.children.consent.title}
      countIndicator={{
        count: unconsentedCount,
        ariaLabel: `${unconsentedCount} ${t.children.consent.unconsented}`
      }}
      opaque
      open={open}
      toggleOpen={() => setOpen(!open)}
      data-qa="collapsible-consents"
      icon={user?.authLevel === 'WEAK' ? faLockAlt : undefined}
    >
      <RequireAuth>
        <ChildConsentsContent childId={childId} />
      </RequireAuth>
    </ResponsiveWholePageCollapsible>
  )
})

const ChildConsentsContent = React.memo(function ChildConsentsContent({
  childId
}: ChildConsentsProps) {
  const t = useTranslation()

  const childConsents = useQueryResult(childConsentsQuery())

  const consents = useMemo(
    () =>
      childConsents.map(
        (consents) =>
          new Map(
            consents[childId]?.map(({ type, given }) => [type, given]) ?? []
          )
      ),
    [childConsents, childId]
  )

  const [form, setForm] =
    useState<
      Record<ChildConsentType, { dirty: boolean; value: boolean | null }>
    >()

  useEffect(() => {
    if (!consents.isSuccess) return

    consents.map((value) =>
      setForm(
        Object.fromEntries(
          childConsentTypes.map((type) => [
            type,
            { dirty: false, value: value.get(type) ?? null }
          ])
        ) as Record<ChildConsentType, { dirty: boolean; value: boolean | null }>
      )
    )
  }, [consents])

  const onSetConsent = useCallback(
    (type: ChildConsentType, given: boolean) =>
      setForm((f) => ({
        ...f,
        [type]: { dirty: true, value: given }
      })),
    []
  )

  // hide section if no consents are enabled
  if (!consents.map((c) => c.size > 0).getOrElse(true)) return null

  return (
    <>
      {form ? (
        <>
          <H3>{t.children.consent.evakaProfilePicture.title}</H3>
          <P>{t.children.consent.evakaProfilePicture.description}</P>
          <LabelLike>
            {t.children.consent.evakaProfilePicture.question}
          </LabelLike>

          <Gap size="m" />
          <FixedSpaceRow>
            <Radio
              checked={form.EVAKA_PROFILE_PICTURE.value === true}
              disabled={
                form.EVAKA_PROFILE_PICTURE.value !== null &&
                !form.EVAKA_PROFILE_PICTURE.dirty
              }
              label={t.common.yes}
              onChange={() => onSetConsent('EVAKA_PROFILE_PICTURE', true)}
              data-qa="consent-profilepic-yes"
            />
            <Radio
              checked={form.EVAKA_PROFILE_PICTURE.value === false}
              disabled={
                form.EVAKA_PROFILE_PICTURE.value !== null &&
                !form.EVAKA_PROFILE_PICTURE.dirty
              }
              label={t.common.no}
              onChange={() => onSetConsent('EVAKA_PROFILE_PICTURE', false)}
              data-qa="consent-profilepic-no"
            />
          </FixedSpaceRow>
          <Gap size="m" />
          {consents
            .map((c) => c.get('EVAKA_PROFILE_PICTURE') === null)
            .getOrElse(true) ? (
            <MutateButton
              primary
              text={t.children.consent.confirm}
              mutation={insertChildConsentsMutation}
              onClick={() =>
                form
                  ? {
                      childId,
                      consents: Object.entries(form)
                        .filter(
                          ([, consent]) =>
                            typeof consent.value === 'boolean' && consent.dirty
                        )
                        .map(([type, consent]) => ({
                          type: type as ChildConsentType,
                          given: consent.value ?? false
                        }))
                    }
                  : cancelMutation
              }
              onSuccess={() => undefined}
              data-qa="consent-confirm"
            />
          ) : null}
        </>
      ) : (
        <SpinnerSegment />
      )}
    </>
  )
})
