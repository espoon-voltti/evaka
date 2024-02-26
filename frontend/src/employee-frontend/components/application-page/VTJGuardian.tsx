// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { isLoading, Loading, Result, wrapResult } from 'lib-common/api'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import ListGrid from 'lib-components/layout/ListGrid'
import { Dimmed, H4, Label } from 'lib-components/typography'

import { getPersonIdentity } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

const getPersonIdentityResult = wrapResult(getPersonIdentity)

async function maybeGetPerson(
  personId: UUID | undefined | null
): Promise<Result<PersonJSON>> {
  if (personId) return getPersonIdentityResult({ personId })
  return Loading.of()
}

interface VTJGuardianProps {
  guardianId: UUID | undefined | null
  otherGuardianLivesInSameAddress?: boolean
}

export default React.memo(function VTJGuardian({
  guardianId,
  otherGuardianLivesInSameAddress
}: VTJGuardianProps) {
  const { i18n } = useTranslation()
  const [guardianResult] = useApiState(
    () => maybeGetPerson(guardianId),
    [guardianId]
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
                {formatName(guardian.firstName, guardian.lastName, i18n, true)}
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
