// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'

import { client } from 'employee-frontend/api/client'
import { ChildState, ChildContext } from 'employee-frontend/state/child'
import { Failure, Result, Success } from 'lib-common/api'
import {
  ChildConsent,
  ChildConsentType,
  childConsentTypes,
  UpdateChildConsentRequest
} from 'lib-common/generated/api-types/children'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Radio from 'lib-components/atoms/form/Radio'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Dimmed, H2, H3, LabelLike, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

export interface Props {
  id: UUID
  startOpen: boolean
}

export function getChildConsents(
  childId: UUID
): Promise<Result<ChildConsent[]>> {
  return client
    .get<JsonOf<ChildConsent[]>>(`/children/${childId}/consent`)
    .then((res) =>
      Success.of(
        res.data.map((consent) => ({
          ...consent,
          givenAt:
            consent.givenAt === null
              ? null
              : HelsinkiDateTime.parseIso(consent.givenAt)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export function modifyChildConsents(
  childId: UUID,
  request: UpdateChildConsentRequest[]
): Promise<Result<void>> {
  return client
    .post(`/children/${childId}/consent`, request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export default React.memo(function ChildConsentsSection({
  id,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext<ChildState>(ChildContext)

  const [childConsents, refreshChildConsents] = useApiState(
    () => getChildConsents(id),
    [id]
  )

  const [open, setOpen] = useState(startOpen)

  const [form, setForm] = useState<Record<ChildConsentType, boolean | null>>()

  const consents = useMemo(
    () =>
      childConsents.map(
        (consents) =>
          new Map(consents.map((consent) => [consent.type, consent]) ?? [])
      ),
    [childConsents]
  )

  useEffect(() => {
    if (!consents.isSuccess) return

    consents.map((value) =>
      setForm(
        Object.fromEntries(
          childConsentTypes.map((type) => [
            type,
            value.get(type)?.given ?? null
          ])
        ) as Record<ChildConsentType, boolean | null>
      )
    )
  }, [consents])

  const evakaProfilePicture = consents
    .getOrElse(undefined)
    ?.get('EVAKA_PROFILE_PICTURE')

  const onSetConsent = useCallback(
    (type: ChildConsentType, given: boolean | null) =>
      setForm((f) => ({
        ...f,
        [type]: given
      })),
    []
  )

  const onConfirm = useCallback(() => {
    if (!form) {
      return Promise.resolve(Failure.of({ message: 'Form not loaded' }))
    }

    return modifyChildConsents(
      id,
      Object.entries(form).map(([type, consent]) => ({
        type: type as ChildConsentType,
        given: consent
      }))
    )
  }, [id, form])

  const unconsentedCount = useMemo(
    () =>
      consents
        .map(
          (consents) =>
            Array.from(consents.values()).filter(({ given }) => given === null)
              .length
        )
        .getOrElse(0),
    [consents]
  )

  // hide section if no consents are enabled
  if (!consents.map((c) => c.size > 0).getOrElse(true)) return null

  return (
    <CollapsibleContentArea
      title={<H2 noMargin>{i18n.childInformation.childConsents.title}</H2>}
      countIndicator={unconsentedCount}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      paddingVertical="L"
      data-qa="consents-collapsible"
    >
      {form ? (
        <>
          <H3>
            {i18n.childInformation.childConsents.evakaProfilePicture.title}
          </H3>
          <P>
            {
              i18n.childInformation.childConsents.evakaProfilePicture
                .description
            }
          </P>
          <LabelLike>
            {i18n.childInformation.childConsents.evakaProfilePicture.question}
          </LabelLike>
          <Gap size="m" />
          <FixedSpaceRow>
            <Radio
              checked={form.EVAKA_PROFILE_PICTURE === true}
              disabled={!permittedActions.has('UPSERT_CHILD_CONSENT')}
              label={i18n.common.yes}
              onChange={() => onSetConsent('EVAKA_PROFILE_PICTURE', true)}
              data-qa="consent-profilepic-yes"
            />
            <Radio
              checked={form.EVAKA_PROFILE_PICTURE === false}
              disabled={!permittedActions.has('UPSERT_CHILD_CONSENT')}
              label={i18n.common.no}
              onChange={() => onSetConsent('EVAKA_PROFILE_PICTURE', false)}
              data-qa="consent-profilepic-no"
            />
            {form.EVAKA_PROFILE_PICTURE !== null && (
              <>
                <Gap horizontal size="L" />
                <InlineButton
                  icon={faTimes}
                  text={i18n.childInformation.childConsents.clearAnswer}
                  onClick={() => onSetConsent('EVAKA_PROFILE_PICTURE', null)}
                  data-qa="consent-profilepic-clear"
                />
              </>
            )}
          </FixedSpaceRow>
          {evakaProfilePicture && evakaProfilePicture.given !== null && (
            <>
              <Gap size="m" />
              <Dimmed data-qa="consent-profilepic-modified-by">
                {i18n.childInformation.childConsents.modifiedBy}:{' '}
                {evakaProfilePicture.givenAt?.toLocalDate().format()}{' '}
                {evakaProfilePicture.givenByEmployee ??
                  `${evakaProfilePicture.givenByGuardian ?? ''} (${
                    i18n.childInformation.childConsents.guardian
                  })`}
              </Dimmed>
            </>
          )}
          <Gap size="m" />
          <AsyncButton
            primary
            text={i18n.childInformation.childConsents.updateAnswer}
            onClick={onConfirm}
            onSuccess={refreshChildConsents}
            data-qa="consent-confirm"
          />
        </>
      ) : (
        <SpinnerSegment />
      )}
    </CollapsibleContentArea>
  )
})
