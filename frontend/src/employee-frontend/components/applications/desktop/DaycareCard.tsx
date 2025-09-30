// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

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

import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { getPlacementDesktopDaycareQuery } from '../queries'

import ApplicationCardPlaced from './ApplicationCardPlaced'

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
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const unitDetails = useQueryResult(
    getPlacementDesktopDaycareQuery({ unitId: daycare.id })
  )

  const placementDraftsWithApplications = useMemo(
    () =>
      unitDetails.map((unitDetails) =>
        unitDetails.placementDrafts.map((placementDraft) => {
          const application = applications.find(
            (a) => a.id === placementDraft.applicationId
          )
          return {
            placementDraft,
            application
          }
        })
      ),
    [unitDetails, applications]
  )
  const [placementDraftsOpen, { toggle: togglePlacementDraftsOpen }] =
    useBoolean(true)

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
            aria-label={i18n.applications.placementDesktop.hideUnit}
            onClick={onRemoveFromShownDaycares}
          />
        </FixedSpaceRow>

        <FixedSpaceRow>
          <FixedSpaceColumn spacing="xxs">
            <Label>
              {i18n.applications.placementDesktop.occupancyConfirmed}
            </Label>
            <span>?? %</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.applications.placementDesktop.occupancyPlanned}</Label>
            <span>?? %</span>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.applications.placementDesktop.occupancyDraft}</Label>
            <span>?? %</span>
          </FixedSpaceColumn>
        </FixedSpaceRow>

        {renderResult(
          placementDraftsWithApplications,
          (placementDraftsWithApplications) => (
            <CollapsibleContentArea
              opaque
              open={placementDraftsOpen}
              toggleOpen={togglePlacementDraftsOpen}
              title={
                <Label>
                  {i18n.applications.placementDesktop.placementDrafts}
                </Label>
              }
              countIndicator={placementDraftsWithApplications.length}
              countIndicatorColor={colors.accents.a8lightBlue}
              paddingHorizontal="zero"
              paddingVertical="zero"
            >
              <ApplicationsWrapper>
                <FixedSpaceColumn>
                  {orderBy(
                    placementDraftsWithApplications,
                    (pd) => pd.placementDraft.childName
                  ).map(({ placementDraft, application }) => (
                    <ApplicationCardPlaced
                      key={placementDraft.applicationId}
                      placementDraft={placementDraft}
                      application={application}
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
