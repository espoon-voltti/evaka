// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'

import ResponsiveWholePageCollapsible from 'citizen-frontend/children/ResponsiveWholePageCollapsible'
import { Failure } from 'lib-common/api'
import {
  ChildConsentType,
  childConsentTypes
} from 'lib-common/generated/api-types/children'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Radio from 'lib-components/atoms/form/Radio'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, LabelLike, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../localization'

import { childConsentsQuery, insertChildConsentsMutation } from './queries'

interface ChildConsentsProps {
  childId: UUID
}

export default React.memo(function ChildConsentsSection({
  childId
}: ChildConsentsProps) {
  const t = useTranslation()

  const childConsents = useQueryResult(childConsentsQuery)
  const { mutateAsync: insertChildConsents } = useMutationResult(
    insertChildConsentsMutation
  )

  const [open, setOpen] = useState(false)

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

  const unconsentedCount = useMemo(
    () =>
      consents
        .map(
          (consents) =>
            Array.from(consents.values()).filter((v) => v === null).length
        )
        .getOrElse(0),
    [consents]
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

  const onConfirm = useCallback(() => {
    if (!form) {
      return Promise.resolve(Failure.of({ message: 'Form not loaded' }))
    }

    return insertChildConsents({
      childId,
      consents: Object.entries(form)
        .filter(
          ([, consent]) => typeof consent.value === 'boolean' && consent.dirty
        )
        .map(([type, consent]) => ({
          type: type as ChildConsentType,
          given: consent.value as boolean
        }))
    })
  }, [childId, form, insertChildConsents])

  // hide section if no consents are enabled
  if (!consents.map((c) => c.size > 0).getOrElse(true)) return null

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
    >
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
              checked={form['EVAKA_PROFILE_PICTURE'].value === true}
              disabled={
                form['EVAKA_PROFILE_PICTURE'].value !== null &&
                !form['EVAKA_PROFILE_PICTURE'].dirty
              }
              label={t.common.yes}
              onChange={() => onSetConsent('EVAKA_PROFILE_PICTURE', true)}
              data-qa="consent-profilepic-yes"
            />
            <Radio
              checked={form['EVAKA_PROFILE_PICTURE'].value === false}
              disabled={
                form['EVAKA_PROFILE_PICTURE'].value !== null &&
                !form['EVAKA_PROFILE_PICTURE'].dirty
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
            <AsyncButton
              primary
              text={t.children.consent.confirm}
              onClick={onConfirm}
              onSuccess={() => undefined}
              data-qa="consent-confirm"
            />
          ) : null}
        </>
      ) : (
        <SpinnerSegment />
      )}
    </ResponsiveWholePageCollapsible>
  )
})
