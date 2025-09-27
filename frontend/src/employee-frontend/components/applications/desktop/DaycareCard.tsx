// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type {
  ApplicationSummary,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faEyeSlash } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { getPlacementDesktopDaycareQuery } from '../queries'

import ApplicationCardMini from './ApplicationCardMini'

export default React.memo(function DaycareCard({
  daycare,
  applications,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure,
  onRemoveFromShownDaycares
}: {
  daycare: PreferredUnit
  applications: ApplicationSummary[]
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
  onRemoveFromShownDaycares: () => void
}) {
  const { colors } = useTheme()
  const unitDetails = useQueryResult(
    getPlacementDesktopDaycareQuery({ unitId: daycare.id })
  )

  const placementDraftsWithApplication = useMemo(
    () =>
      unitDetails.map((unitDetails) =>
        unitDetails.placementDrafts.flatMap((pd) => {
          const application = applications.find(
            (a) => a.id === pd.applicationId
          )
          return application ? [{ ...pd, application }] : []
        })
      ),
    [unitDetails, applications]
  )
  const [
    placementDraftsWithApplicationOpen,
    { toggle: togglePlacementDraftsWithApplication }
  ] = useBoolean(true)

  const otherPlacementDrafts = useMemo(
    () =>
      unitDetails.map((unitDetails) =>
        unitDetails.placementDrafts.filter(
          (pd) => !applications.some((a) => a.id === pd.applicationId)
        )
      ),
    [unitDetails, applications]
  )
  const [otherPlacementDraftsOpen, { toggle: toggleOtherPlacementDrafts }] =
    useBoolean(false)

  return (
    <Card>
      <FixedSpaceColumn spacing="s">
        <FixedSpaceRow justifyContent="space-between">
          <a
            href={`/employee/units/${daycare.id}`}
            target="_blank"
            rel="noreferrer"
          >
            <H4 noMargin style={{ color: colors.main.m1, fontWeight: 600 }}>
              {daycare.name}
            </H4>
          </a>
          <IconOnlyButton
            icon={faEyeSlash}
            aria-label="Piilota"
            onClick={onRemoveFromShownDaycares}
          />
        </FixedSpaceRow>

        <FixedSpaceRow>
          <FixedSpaceColumn spacing="xxs">
            <Label>Vahvistettu</Label>
            <span>?? %</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>Suunniteltu</Label>
            <span>?? %</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>Hahmoteltu</Label>
            <span>?? %</span>
          </FixedSpaceColumn>
        </FixedSpaceRow>

        {renderResult(
          combine(placementDraftsWithApplication, otherPlacementDrafts),
          ([placementDraftsWithApplication, otherPlacementDrafts]) => (
            <FixedSpaceColumn spacing="s">
              <CollapsibleContentArea
                opaque
                open={placementDraftsWithApplicationOpen}
                toggleOpen={togglePlacementDraftsWithApplication}
                title={<Label>Sijoitushahmotelmat (valitut hakemukset)</Label>}
                countIndicator={placementDraftsWithApplication.length}
                countIndicatorColor={colors.accents.a8lightBlue}
                paddingHorizontal="zero"
                paddingVertical="zero"
              >
                <ApplicationsWrapper>
                  <FixedSpaceColumn>
                    {orderBy(
                      placementDraftsWithApplication,
                      (pd) => pd.childName
                    ).map((pd) => (
                      <ApplicationCardMini
                        key={pd.application.id}
                        application={pd.application}
                        unitId={daycare.id}
                        onUpdateApplicationPlacementSuccess={
                          onUpdateApplicationPlacementSuccess
                        }
                        onUpdateApplicationPlacementFailure={
                          onUpdateApplicationPlacementFailure
                        }
                      />
                    ))}
                  </FixedSpaceColumn>
                </ApplicationsWrapper>
              </CollapsibleContentArea>

              <CollapsibleContentArea
                opaque
                open={otherPlacementDraftsOpen}
                toggleOpen={toggleOtherPlacementDrafts}
                title={<Label>Sijoitushahmotelmat (muut)</Label>}
                countIndicator={otherPlacementDrafts.length}
                countIndicatorColor={colors.accents.a8lightBlue}
                paddingHorizontal="zero"
                paddingVertical="zero"
              >
                <ApplicationsWrapper>
                  <FixedSpaceColumn spacing="xs">
                    {orderBy(otherPlacementDrafts, (pd) => pd.childName).map(
                      (pd) => (
                        <FixedSpaceRow key={pd.applicationId}>
                          <span>{pd.childName}</span>
                        </FixedSpaceRow>
                      )
                    )}
                  </FixedSpaceColumn>
                </ApplicationsWrapper>
              </CollapsibleContentArea>
            </FixedSpaceColumn>
          )
        )}
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div`
  min-width: 450px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const ApplicationsWrapper = styled.div`
  padding: 0 ${defaultMargins.s};
`
