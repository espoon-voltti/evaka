// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { getPerson } from '../../api/person'
import { Loading, Result } from 'lib-common/api'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { Dimmed, H4, Label } from 'lib-components/typography'
import ListGrid from 'lib-components/layout/ListGrid'
import { Link } from 'react-router-dom'
import { formatName } from '../../utils'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { UUID } from 'lib-common/types'

interface VTJGuardianProps {
  guardianId: UUID | undefined | null
  otherGuardianLivesInSameAddress?: boolean
}

function VTJGuardian({
  guardianId,
  otherGuardianLivesInSameAddress
}: VTJGuardianProps) {
  const { i18n } = useTranslation()
  const [guardianResult, setGuardian] = useState<Result<PersonJSON>>(
    Loading.of()
  )

  useEffect(() => {
    if (guardianId) {
      void getPerson(guardianId).then(setGuardian)
    }
  }, [guardianId])

  const render = (guardian: PersonJSON) => (
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

      {otherGuardianLivesInSameAddress !== undefined && (
        <>
          <Label>{i18n.application.guardians.secondGuardian.sameAddress}</Label>
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
