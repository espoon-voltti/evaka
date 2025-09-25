// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import type { ApplicationSummary } from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faArrowLeft,
  faCommentAlt,
  faFile,
  faPlay,
  fasCommentAltLines,
  faSection
} from 'lib-icons'
import { faUndo } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import { CareTypeChip } from '../../common/CareTypeLabel'
import { BasisFragment, DateOfBirthInfo } from '../ApplicationsList'
import { updateApplicationTrialPlacementMutation } from '../queries'

export default React.memo(function ApplicationCard({
  application,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure
}: {
  application: ApplicationSummary
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unitId: DaycareId | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <Card>
      <FixedSpaceColumn spacing="s">
        <FixedSpaceRow alignItems="flex-start" justifyContent="space-between">
          <FixedSpaceRow alignItems="center" spacing="xs">
            <PlacementCircle
              type={
                isPartDayPlacement(application.placementType) ? 'half' : 'full'
              }
              label={
                application.serviceNeed !== null
                  ? application.serviceNeed.nameFi
                  : i18n.placement.type[application.placementType]
              }
            />
            <LabelLike>
              {application.firstName} {application.lastName}
            </LabelLike>
          </FixedSpaceRow>
          <FixedSpaceRow spacing="L" alignItems="center">
            <CareTypeChip type={application.placementType} />
            <FixedSpaceRow spacing="s" alignItems="center">
              <a
                href={`/employee/applications/${application.id}`}
                target="_blank"
                rel="noreferrer"
              >
                <IconOnlyButton icon={faFile} aria-label={i18n.common.open} />
              </a>
              <Tooltip
                tooltip={
                  application.serviceWorkerNote ? (
                    <span>{application.serviceWorkerNote}</span>
                  ) : (
                    <i>{i18n.applications.list.addNote}</i>
                  )
                }
              >
                <IconOnlyButton
                  icon={
                    application.serviceWorkerNote
                      ? fasCommentAltLines
                      : faCommentAlt
                  }
                  onClick={(e) => {
                    e.stopPropagation()
                    // todo
                  }}
                  aria-label={
                    application.serviceWorkerNote
                      ? i18n.common.edit
                      : i18n.applications.list.addNote
                  }
                  data-qa="service-worker-note"
                />
              </Tooltip>
            </FixedSpaceRow>
          </FixedSpaceRow>
        </FixedSpaceRow>
        <FixedSpaceRow>
          <div style={{ width: '25%' }}>
            <DateOfBirthInfo application={application} />
          </div>
          <FixedSpaceRow
            spacing="xs"
            alignItems="center"
            style={{ width: '25%' }}
          >
            <RoundIcon content={faSection} color={colors.main.m1} size="m" />
            <div>{application.dueDate?.format() ?? '-'}</div>
          </FixedSpaceRow>
          <FixedSpaceRow
            spacing="xs"
            alignItems="center"
            style={{ width: '25%' }}
          >
            <RoundIcon content={faPlay} color={colors.main.m1} size="m" />
            <div>{application.startDate?.format() ?? '-'}</div>
          </FixedSpaceRow>
        </FixedSpaceRow>
        <FixedSpaceRow>
          <FixedSpaceColumn spacing="xs" style={{ width: '70%' }}>
            <LabelLike>Hakutoiveet</LabelLike>
            <FixedSpaceColumn spacing="xxs">
              {application.preferredUnits.map((unit, index) => (
                <FixedSpaceRow key={index}>
                  <UnitListItem
                    $current={application.trialPlacementUnit === unit.id}
                  >
                    {index + 1}. {unit.name}
                  </UnitListItem>
                  <MutateButton
                    appearance="inline"
                    text={
                      application.trialPlacementUnit === unit.id
                        ? 'Palauta'
                        : 'Lisää'
                    }
                    icon={
                      application.trialPlacementUnit === unit.id
                        ? faUndo
                        : faArrowLeft
                    }
                    mutation={updateApplicationTrialPlacementMutation}
                    onClick={() => ({
                      applicationId: application.id,
                      body: {
                        trialUnitId:
                          application.trialPlacementUnit === unit.id
                            ? null
                            : unit.id
                      }
                    })}
                    onSuccess={() => {
                      onUpdateApplicationPlacementSuccess(
                        application.id,
                        application.trialPlacementUnit === unit.id
                          ? null
                          : unit.id
                      )
                    }}
                    onFailure={() => {
                      onUpdateApplicationPlacementFailure()
                    }}
                  />
                </FixedSpaceRow>
              ))}
            </FixedSpaceColumn>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xs">
            <LabelLike>Perusteet</LabelLike>
            <FixedSpaceFlexWrap horizontalSpacing="xs">
              <BasisFragment application={application} />
            </FixedSpaceFlexWrap>
          </FixedSpaceColumn>
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div`
  width: 580px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const UnitListItem = styled.span<{ $current: boolean }>`
  width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-left: ${defaultMargins.xs};

  ${(p) =>
    p.$current
      ? css`
          font-weight: 600;
        `
      : ''}
`
