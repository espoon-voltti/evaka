// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { UUID } from 'types'
import { getPersonDetails } from 'api/person'
import { Loading, Result } from 'api'
import { PersonDetails } from 'types/person'
import { H4, Label } from '@evaka/lib-components/src/typography'
import { renderResult } from 'components/shared/atoms/state/async-rendering'
import ListGrid from 'components/shared/layout/ListGrid'
import { Link } from 'react-router-dom'
import { formatName } from 'utils'
import { useTranslation } from 'state/i18n'

interface VTJGuardianProps {
  guardianId: UUID | undefined | null
  otherGuardianLivesInSameAddress: boolean
}

function VTJGuardian({
  guardianId,
  otherGuardianLivesInSameAddress
}: VTJGuardianProps) {
  const { i18n } = useTranslation()
  const [guardianResult, setGuardian] = useState<Result<PersonDetails>>(
    Loading.of()
  )

  useEffect(() => {
    if (guardianId) {
      void getPersonDetails(guardianId).then(setGuardian)
    }
  }, [guardianId])

  const render = (guardian: PersonDetails) => (
    <ListGrid>
      <Label>{i18n.application.person.name}</Label>
      <Link to={`/profile/${guardian.id}`} data-qa="vtj-guardian-name">
        <span data-qa="link-vtj-guardian-name">
          {formatName(guardian.firstName, guardian.lastName, i18n, true)}
        </span>
      </Link>
      <Label>{i18n.application.person.ssn}</Label>
      <span data-qa="vtj-guardian-ssn">{guardian.socialSecurityNumber}</span>

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
          }, ${guardian.postalCode ?? ''} ${guardian.postOffice ?? ''}`}</span>
        </>
      )}

      <Label>{i18n.application.person.phone}</Label>
      <span data-qa="vtj-guardian-phone">{guardian.phone}</span>
      <Label>{i18n.application.person.email}</Label>
      <span data-qa="vtj-guardian-email">{guardian.email}</span>
      <Label>{i18n.application.guardians.secondGuardian.sameAddress}</Label>
      <span data-qa="other-vtj-guardian-lives-in-same-address">
        {otherGuardianLivesInSameAddress ? i18n.common.yes : i18n.common.no}
      </span>
    </ListGrid>
  )

  return (
    <div>
      <H4>{i18n.application.guardians.vtjGuardian}</H4>
      {guardianId ? (
        renderResult(guardianResult, render)
      ) : (
        <span data-qa="no-other-vtj-guardian">
          {i18n.application.guardians.secondGuardian.noVtjGuardian}
        </span>
      )}
    </div>
  )
}

export default VTJGuardian
