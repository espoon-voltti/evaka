// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'wouter'

import { isLoading } from 'lib-common/api'
import type { PersonJSON } from 'lib-common/generated/api-types/pis'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { pendingQuery, useQueryResult } from 'lib-common/query'
import ListGrid from 'lib-components/layout/ListGrid'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { Dimmed, H4, Label } from 'lib-components/typography'

import { personIdentityQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

interface VTJGuardianProps {
  guardianId: PersonId | undefined | null
  otherGuardianLivesInSameAddress?: boolean
}

export default React.memo(function VTJGuardian({
  guardianId,
  otherGuardianLivesInSameAddress
}: VTJGuardianProps) {
  const { i18n } = useTranslation()
  const guardianResult = useQueryResult(
    guardianId
      ? personIdentityQuery({ personId: guardianId })
      : pendingQuery<PersonJSON>()
  )

  return (
    <div
      data-qa="vtj-guardian-section"
      data-isloading={!!guardianId && isLoading(guardianResult)}
    >
      <H4>{i18n.application.guardians.vtjGuardian}</H4>
      {guardianId ? (
        renderResult(guardianResult, (guardian: PersonJSON) => (
          <ListGrid>
            <Label>{i18n.application.person.name}</Label>
            <Link to={`/profile/${guardian.id}`} data-qa="vtj-guardian-name">
              <span data-qa="link-vtj-guardian-name">
                <PersonName person={guardian} format="Last First" />
              </span>
            </Link>
            <Label>{i18n.application.person.ssn}</Label>
            <span data-qa="vtj-guardian-ssn">
              {guardian.socialSecurityNumber}
            </span>

            {guardian.restrictedDetailsEnabled ? (
              <>
                <Label>{i18n.application.person.address}</Label>
                <span>{i18n.application.person.restricted}</span>
              </>
            ) : (
              <>
                <Label>{i18n.application.person.address}</Label>
                <span data-qa="vtj-guardian-address">{`${
                  guardian.streetAddress ?? ''
                }, ${guardian.postalCode ?? ''} ${
                  guardian.postOffice ?? ''
                }`}</span>
              </>
            )}

            <Label>{i18n.application.person.phone}</Label>
            <span data-qa="vtj-guardian-phone">{guardian.phone}</span>
            <Label>{i18n.application.person.email}</Label>
            <span data-qa="vtj-guardian-email">{guardian.email}</span>

            {otherGuardianLivesInSameAddress !== undefined && (
              <>
                <Label>
                  {i18n.application.guardians.secondGuardian.sameAddress}
                </Label>
                {otherGuardianLivesInSameAddress ? (
                  <span data-qa="other-vtj-guardian-lives-in-same-address">
                    {i18n.common.yes}
                  </span>
                ) : (
                  <Dimmed data-qa="other-vtj-guardian-lives-in-same-address">
                    {i18n.common.no}
                  </Dimmed>
                )}
              </>
            )}
          </ListGrid>
        ))
      ) : (
        <span data-qa="no-other-vtj-guardian">
          {i18n.application.guardians.secondGuardian.noVtjGuardian}
        </span>
      )}
    </div>
  )
})
