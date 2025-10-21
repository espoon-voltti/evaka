// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import type { RefObject } from 'react'
import React, { useContext, useMemo } from 'react'
import styled, { css, keyframes, useTheme } from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type {
  ApplicationSummary,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H4, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faLineChart } from 'lib-icons'
import { faEyeSlash } from 'lib-icons'
import { fasExclamation } from 'lib-icons'

import { ApplicationUIContext } from '../../../state/application-ui'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import { getPlacementDesktopDaycareQuery } from '../queries'

import ApplicationCardPlaced from './ApplicationCardPlaced'
import OccupancyModal from './OccupancyModal'

export default React.memo(function DaycareCard({
  daycare,
  highlighted,
  applications,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onRemoveFromShownDaycares,
  ref
}: {
  daycare: PreferredUnit
  highlighted: boolean
  applications: ApplicationSummary[]
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
  onRemoveFromShownDaycares: () => void
  ref: RefObject<HTMLDivElement | null> | undefined
}) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const { occupancyPeriodStart } = useContext(ApplicationUIContext)
  const unitDetails = useQueryResult(
    getPlacementDesktopDaycareQuery({
      unitId: daycare.id,
      occupancyStart: occupancyPeriodStart
    })
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

  const [isGraphOpen, { on: openGraph, off: closeGraph }] = useBoolean(false)

  const [placementDraftsOpen, { toggle: togglePlacementDraftsOpen }] =
    useBoolean(true)

  return (
    <Card ref={ref} $highlighted={highlighted}>
      {isGraphOpen && unitDetails.isSuccess && (
        <OccupancyModal daycare={unitDetails.value} onClose={closeGraph} />
      )}
      <FixedSpaceColumn spacing="s">
        <FixedSpaceRow justifyContent="space-between">
          <FixedSpaceRow>
            <a
              href={`/employee/units/${daycare.id}?period=${encodeURIComponent('3 months')}&startDate=${occupancyPeriodStart.formatIso()}`}
              target="_blank"
              rel="noreferrer"
            >
              <H4 noMargin style={{ color: colors.main.m1, fontWeight: 600 }}>
                {daycare.name}
              </H4>
            </a>
            {unitDetails.isSuccess && !!unitDetails.value.serviceWorkerNote && (
              <Tooltip
                tooltip={unitDetails.value.serviceWorkerNote}
                width="large"
              >
                <RoundIcon
                  content={fasExclamation}
                  color={colors.status.warning}
                  textColor={colors.grayscale.g0}
                  size="m"
                />
              </Tooltip>
            )}
          </FixedSpaceRow>
          <FixedSpaceRow spacing="xs">
            <Tooltip
              tooltip={i18n.applications.placementDesktop.openGraph}
              delayed
            >
              <IconOnlyButton
                icon={faLineChart}
                aria-label={i18n.applications.placementDesktop.openGraph}
                onClick={openGraph}
                disabled={!unitDetails.isSuccess || unitDetails.isReloading}
              />
            </Tooltip>
            <Tooltip
              tooltip={i18n.applications.placementDesktop.hideUnit}
              delayed
            >
              <IconOnlyButton
                icon={faEyeSlash}
                aria-label={i18n.applications.placementDesktop.hideUnit}
                onClick={onRemoveFromShownDaycares}
              />
            </Tooltip>
          </FixedSpaceRow>
        </FixedSpaceRow>

        {renderResult(
          combine(unitDetails, placementDraftsWithApplications),
          ([unitDetails, placementDraftsWithApplications]) => (
            <>
              <FixedSpaceRow>
                <FixedSpaceColumn spacing="xxs">
                  <Label>
                    {i18n.applications.placementDesktop.occupancyConfirmed}
                  </Label>
                  <span>
                    {unitDetails.occupancyConfirmed?.max?.percentage ?? '??'} %
                  </span>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>
                    {i18n.applications.placementDesktop.occupancyPlanned}
                  </Label>
                  <span>
                    {unitDetails.occupancyPlanned?.max?.percentage ?? '??'} %
                  </span>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>
                    {i18n.applications.placementDesktop.occupancyDraft}
                  </Label>
                  <span>
                    {unitDetails.occupancyDraft?.max?.percentage ?? '??'} %
                  </span>
                </FixedSpaceColumn>
              </FixedSpaceRow>
              <Gap size="xs" />
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
                        onDeleteApplicationPlacementSuccess={
                          onDeleteApplicationPlacementSuccess
                        }
                        onMutateApplicationPlacementFailure={
                          onMutateApplicationPlacementFailure
                        }
                      />
                    ))}
                  </FixedSpaceColumn>
                </ApplicationsWrapper>
              </CollapsibleContentArea>
            </>
          )
        )}
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div<{ $highlighted: boolean }>`
  min-width: 400px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  ${(p) =>
    p.$highlighted
      ? css`
          animation: ${blinkShadow} 1s ease-in-out 1;
        `
      : ''}
`

const ApplicationsWrapper = styled.div`
  padding: 0 ${defaultMargins.s};
`

const blinkShadow = keyframes`
  0% {
    box-shadow: 0 0 0 0 rgba(77, 127, 204, 0);
  }
  30% {
    box-shadow: 0 0 0 4px rgba(77, 127, 204, 0.8);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(77, 127, 204, 0);
  }
`
