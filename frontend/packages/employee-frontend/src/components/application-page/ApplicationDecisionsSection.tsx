// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faFilePdf, faGavel } from 'icon-set'
import { Label } from '~components/shared/Typography'
import CollapsibleSection from '~components/shared/molecules/CollapsibleSection'
import ListGrid from '~components/shared/layout/ListGrid'
import { useTranslation } from '~state/i18n'
import { Decision } from 'types/decision'
import { Link } from 'react-router-dom'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import DecisionResponse from 'components/application-page/DecisionResponse'
import { UUID } from 'types'

type Props = {
  applicationId: UUID
  decisions: Decision[]
  reloadApplication: () => void
}

const isPending = (decision: Decision) => decision.status === 'PENDING'

const isBlocked = (decisions: Decision[], decision: Decision) =>
  ['PRESCHOOL_DAYCARE'].includes(decision.type) &&
  decisions.length > 1 &&
  !decisions.find(
    (d) =>
      (d.type === 'PRESCHOOL' || d.type === 'PREPARATORY_EDUCATION') &&
      (d.status === 'ACCEPTED' || d.status === 'REJECTED')
  )

export default React.memo(function ApplicationDecisionsSection({
  applicationId,
  decisions,
  reloadApplication
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection title={i18n.application.decisions.title} icon={faGavel}>
      {decisions.length === 0 ? (
        <span>{i18n.application.decisions.noDecisions}</span>
      ) : (
        <FixedSpaceColumn spacing="XL">
          {decisions.map((decision) => (
            <ListGrid
              key={decision.id}
              data-qa={`application-decision-${decision.type}`}
            >
              <Label>{i18n.application.decisions.type}</Label>
              <FixedSpaceRow>
                <span>{i18n.application.decisions.types[decision.type]}</span>
                <a
                  href={`/api/internal/decisions2/${decision.id}/download`}
                  target="_blank"
                  rel="noreferrer"
                >
                  <FixedSpaceRow spacing={'xs'} alignItems={'center'}>
                    <FontAwesomeIcon icon={faFilePdf} />
                    <span>{i18n.application.decisions.download}</span>
                  </FixedSpaceRow>
                </a>
              </FixedSpaceRow>

              <Label>{i18n.application.decisions.num}</Label>
              <span>{decision.decisionNumber}</span>

              <Label>{i18n.application.decisions.status}</Label>
              <span>
                {i18n.application.decisions.statuses[decision.status]}
              </span>

              <Label>{i18n.application.decisions.unit}</Label>
              <Link to={`/units/${decision.unit.id}`}>
                {decision.unit.name}
              </Link>

              {isPending(decision) && (
                <>
                  <Label>{i18n.application.decisions.response.label}</Label>

                  {isBlocked(decisions, decision) ? (
                    <span>{i18n.application.decisions.blocked}</span>
                  ) : (
                    <DecisionResponse
                      applicationId={applicationId}
                      decision={decision}
                      reloadApplication={reloadApplication}
                    />
                  )}
                </>
              )}
            </ListGrid>
          ))}
        </FixedSpaceColumn>
      )}
    </CollapsibleSection>
  )
})
