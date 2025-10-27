// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import styled, { css, useTheme } from 'styled-components'
import { useLocation } from 'wouter'

import type {
  ApplicationSummary,
  ApplicationSummaryPlacementDraft,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { UnitStub } from 'lib-common/generated/api-types/daycare'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import { cancelMutation, useMutation } from 'lib-common/query'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H3, LabelLike, Light } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faHeart } from 'lib-icons'
import { faEye } from 'lib-icons'
import {
  faArrowLeft,
  faCommentAlt,
  fasCommentAltLines,
  faSection
} from 'lib-icons'
import { faUndo } from 'lib-icons'
import { faCheck, faPen, faTimes } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import { CareTypeChip } from '../../common/CareTypeLabel'
import {
  BasisFragment,
  DateOfBirthInfo,
  ServiceWorkerNoteModal
} from '../ApplicationsList'
import {
  deleteApplicationPlacementDraftMutation,
  upsertApplicationPlacementDraftMutation
} from '../queries'

export default React.memo(function ApplicationCard({
  application,
  shownDaycares,
  allUnits,
  onUpsertApplicationPlacementSuccess,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onAddOrHighlightDaycare
}: {
  application: ApplicationSummary
  shownDaycares: PreferredUnit[]
  allUnits: UnitStub[]
  onUpsertApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit,
    startDate: LocalDate
  ) => void
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
  onAddOrHighlightDaycare: (unit: PreferredUnit) => void
}) {
  const { i18n } = useTranslation()
  const { colors } = useTheme()
  const [, navigate] = useLocation()

  const {
    mutateAsync: upsertApplicationPlacementDraft,
    isPending: updatePending
  } = useMutation(upsertApplicationPlacementDraftMutation)

  const { isPending: deletePending } = useMutation(
    deleteApplicationPlacementDraftMutation
  )

  const [editingNote, setEditingNote] = useState(false)

  const unitRows = useMemo(
    () => [
      ...application.preferredUnits,
      ...(application.placementDraft &&
      !application.preferredUnits.some(
        (d) => d.id === application.placementDraft?.unit?.id
      )
        ? [application.placementDraft.unit]
        : [])
    ],
    [application.preferredUnits, application.placementDraft]
  )

  return (
    <Card $placed={application.placementDraft !== null}>
      {editingNote && (
        <ServiceWorkerNoteModal
          applicationId={application.id}
          serviceWorkerNote={application.serviceWorkerNote}
          onClose={() => setEditingNote(false)}
        />
      )}
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
            <a
              href={`/employee/applications/${application.id}`}
              target="_blank"
              rel="noreferrer"
            >
              <H3 noMargin style={{ color: colors.main.m1 }}>
                {application.lastName} {application.firstName}
              </H3>
            </a>
          </FixedSpaceRow>
          <FixedSpaceRow spacing="L" alignItems="center">
            <CareTypeChip type={application.placementType} />
            <FixedSpaceRow spacing="xs" alignItems="center">
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
                  onClick={() => setEditingNote(true)}
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

        <DatesAndBasisArea justifyContent="space-between" alignItems="center">
          <FixedSpaceRow>
            <DateCol>
              <Tooltip
                tooltip={i18n.applications.placementDesktop.birthDate}
                delayed
              >
                <DateOfBirthInfo application={application} />
              </Tooltip>
            </DateCol>
            <DateCol>
              <Tooltip
                tooltip={i18n.applications.placementDesktop.dueDate}
                delayed
              >
                <FixedSpaceRow spacing="xs" alignItems="center">
                  <RoundIcon
                    content={faSection}
                    color={colors.grayscale.g15}
                    textColor={colors.grayscale.g100}
                    size="m"
                  />
                  <div>
                    {application.transferApplication ? (
                      <Light>
                        {i18n.applications.placementDesktop.transfer}
                      </Light>
                    ) : (
                      (application.dueDate?.format() ?? '-')
                    )}
                  </div>
                </FixedSpaceRow>
              </Tooltip>
            </DateCol>
            <DateCol>
              <Tooltip
                tooltip={i18n.applications.placementDesktop.preferredStartDate}
                delayed
              >
                <FixedSpaceRow spacing="xs" alignItems="center">
                  <RoundIcon
                    content={faHeart}
                    color={colors.grayscale.g15}
                    textColor={colors.grayscale.g100}
                    size="m"
                  />
                  <div>{application.startDate?.format() ?? '-'}</div>
                </FixedSpaceRow>
              </Tooltip>
            </DateCol>
          </FixedSpaceRow>
          <FixedSpaceRow
            spacing="xs"
            alignItems="center"
            justifyContent="flex-end"
            style={{ flexGrow: 1 }}
          >
            <BasisFragment application={application} />
          </FixedSpaceRow>
        </DatesAndBasisArea>

        <FixedSpaceColumn spacing="xs">
          <LabelLike>
            {i18n.applications.placementDesktop.preferences}
          </LabelLike>
          <FixedSpaceColumn spacing="zero">
            {unitRows.map((unit, index) => (
              <UnitRow
                key={unit.id}
                application={application}
                unit={unit}
                lastRow={index === unitRows.length - 1}
                unitVisible={shownDaycares.some((d) => d.id === unit.id)}
                onUpsertApplicationPlacementSuccess={
                  onUpsertApplicationPlacementSuccess
                }
                onDeleteApplicationPlacementSuccess={
                  onDeleteApplicationPlacementSuccess
                }
                onMutateApplicationPlacementFailure={
                  onMutateApplicationPlacementFailure
                }
                onAddOrHighlightDaycare={onAddOrHighlightDaycare}
              />
            ))}
          </FixedSpaceColumn>
        </FixedSpaceColumn>

        <FixedSpaceRow justifyContent="space-between" alignItems="center">
          <div style={{ width: '360px' }}>
            {application.checkedByAdmin && (
              <Combobox
                items={allUnits}
                selectedItem={null}
                onChange={(unit) => {
                  if (unit) {
                    onAddOrHighlightDaycare(unit)
                    upsertApplicationPlacementDraft({
                      applicationId: application.id,
                      previousUnitId:
                        application.placementDraft?.unit?.id ?? null,
                      body: { unitId: unit.id, startDate: null }
                    })
                      .then(({ startDate }) =>
                        onUpsertApplicationPlacementSuccess(
                          application.id,
                          unit,
                          startDate
                        )
                      )
                      .catch(onMutateApplicationPlacementFailure)
                  }
                }}
                placeholder={
                  i18n.applications.placementDesktop
                    .createPlacementDraftToOtherUnit
                }
                getItemLabel={(unit) => unit.name}
                disabled={updatePending || deletePending}
                fullWidth
              />
            )}
          </div>
          {application.checkedByAdmin ? (
            <Button
              appearance="button"
              text={i18n.applications.placementDesktop.toPlacementPlan}
              onClick={() =>
                navigate(`/applications/${application.id}/placement`)
              }
            />
          ) : (
            <a
              href={`/employee/applications/${application.id}`}
              target="_blank"
              rel="noreferrer"
            >
              <Button
                appearance="button"
                text={i18n.applications.placementDesktop.checkApplication}
              />
            </a>
          )}
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </Card>
  )
})

const UnitRow = React.memo(function UnitRow({
  application,
  unit,
  unitVisible,
  lastRow,
  onUpsertApplicationPlacementSuccess,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onAddOrHighlightDaycare
}: {
  application: ApplicationSummary
  unit: PreferredUnit
  unitVisible: boolean
  lastRow: boolean
  onUpsertApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit,
    startDate: LocalDate
  ) => void
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
  onAddOrHighlightDaycare: (unit: PreferredUnit) => void
}) {
  const { i18n } = useTranslation()
  const preferenceIndex = application.preferredUnits.findIndex(
    (u) => u.id === unit.id
  )
  const placedHere = application.placementDraft?.unit.id === unit.id

  const [editingDate, setEditingDate] = useState(false)

  return (
    <UnitRowContainer
      $placedHere={placedHere}
      $last={lastRow}
      justifyContent="space-between"
      alignItems="center"
    >
      <UnitRowName alignItems="center" spacing="xs">
        <UnitRowLink
          onClick={
            unitVisible ? () => onAddOrHighlightDaycare(unit) : undefined
          }
        >
          {preferenceIndex >= 0
            ? `${preferenceIndex + 1}. ${unit.name}`
            : `${i18n.applications.placementDesktop.other}: ${unit.name}`}
        </UnitRowLink>
        {!unitVisible && application.placementDraft !== null && (
          <Tooltip
            tooltip={i18n.applications.placementDesktop.showUnit}
            delayed
          >
            <IconOnlyButton
              icon={faEye}
              aria-label={i18n.applications.placementDesktop.showUnit}
              onClick={() => onAddOrHighlightDaycare(unit)}
            />
          </Tooltip>
        )}
      </UnitRowName>

      {application.placementDraft &&
        placedHere &&
        (editingDate ? (
          <DateEditor
            applicationId={application.id}
            placementDraft={application.placementDraft}
            onUpsertApplicationPlacementSuccess={
              onUpsertApplicationPlacementSuccess
            }
            onClose={() => setEditingDate(false)}
          />
        ) : (
          <FixedSpaceRow spacing="m">
            {/*minWidth attempts to keep the date from jumping between read and edit*/}
            <FixedSpaceRow spacing="s" style={{ minWidth: '178px' }}>
              <span>{application.placementDraft.startDate.format()} –</span>
              <IconOnlyButton
                icon={faPen}
                onClick={() => setEditingDate(true)}
                aria-label={i18n.common.edit}
              />
            </FixedSpaceRow>
            <Tooltip
              tooltip={i18n.applications.placementDesktop.cancelPlacementDraft}
              delayed
            >
              <MutateIconOnlyButton
                icon={faUndo}
                aria-label={
                  i18n.applications.placementDesktop.cancelPlacementDraft
                }
                mutation={deleteApplicationPlacementDraftMutation}
                onClick={() =>
                  application.placementDraft
                    ? {
                        applicationId: application.id,
                        previousUnitId: application.placementDraft.unit.id
                      }
                    : cancelMutation
                }
                onSuccess={() =>
                  onDeleteApplicationPlacementSuccess(application.id)
                }
                onFailure={onMutateApplicationPlacementFailure}
                successTimeout={0}
              />
            </Tooltip>
          </FixedSpaceRow>
        ))}

      {application.placementDraft === null &&
        (unitVisible ? (
          <MutateButton
            appearance="inline"
            text={i18n.applications.placementDesktop.createPlacementDraft}
            icon={faArrowLeft}
            mutation={upsertApplicationPlacementDraftMutation}
            onClick={() => ({
              applicationId: application.id,
              previousUnitId: null,
              body: {
                unitId: unit.id,
                startDate: null
              }
            })}
            onSuccess={({ startDate }) =>
              onUpsertApplicationPlacementSuccess(
                application.id,
                unit,
                startDate
              )
            }
            onFailure={onMutateApplicationPlacementFailure}
            successTimeout={0}
          />
        ) : (
          <Button
            appearance="inline"
            icon={faEye}
            text={i18n.applications.placementDesktop.show}
            onClick={() => onAddOrHighlightDaycare(unit)}
          />
        ))}
    </UnitRowContainer>
  )
})

const DateEditor = React.memo(function DateEditor({
  applicationId,
  placementDraft,
  onUpsertApplicationPlacementSuccess,
  onClose
}: {
  applicationId: ApplicationId
  placementDraft: ApplicationSummaryPlacementDraft
  onUpsertApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit,
    startDate: LocalDate
  ) => void
  onClose: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [date, setDate] = useState(placementDraft.startDate)

  return (
    <FixedSpaceRow alignItems="center">
      <DatePicker
        date={date}
        onChange={(val) => {
          if (val) setDate(val)
        }}
        locale={lang}
      />
      <FixedSpaceRow spacing="xs" alignItems="center">
        <MutateIconOnlyButton
          icon={faCheck}
          mutation={upsertApplicationPlacementDraftMutation}
          onClick={() => ({
            applicationId: applicationId,
            previousUnitId: placementDraft.unit.id,
            body: { unitId: placementDraft.unit.id, startDate: date }
          })}
          onSuccess={({ startDate }) => {
            onUpsertApplicationPlacementSuccess(
              applicationId,
              placementDraft.unit,
              startDate
            )
            onClose()
          }}
          aria-label={i18n.common.edit}
        />
        <IconOnlyButton
          icon={faTimes}
          onClick={onClose}
          aria-label={i18n.common.cancel}
        />
      </FixedSpaceRow>
    </FixedSpaceRow>
  )
})

const Card = styled.div<{ $placed: boolean }>`
  flex-grow: 1;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  border-left: 4px solid
    ${(p) =>
      p.$placed ? p.theme.colors.grayscale.g15 : p.theme.colors.main.m3};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const DatesAndBasisArea = styled(FixedSpaceRow)`
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  padding: ${defaultMargins.xs};
`

const DateCol = styled.div`
  width: 120px;
`

const UnitRowContainer = styled(FixedSpaceRow)<{
  $placedHere: boolean
  $last: boolean
}>`
  height: 40px;
  padding: 0 ${defaultMargins.xs};
  border-left: 4px solid transparent;
  border-radius: 4px;
  ${(p) =>
    p.$placedHere
      ? css`
          background-color: #dfe9f6;
          border-left: 4px solid ${p.theme.colors.main.m3};
          font-weight: 600;
        `
      : p.$last
        ? ''
        : css`
            border-bottom: 1px solid ${p.theme.colors.grayscale.g15};
          `}
`

const UnitRowName = styled(FixedSpaceRow)`
  max-width: 180px;
  @media (min-width: 1407px) {
    max-width: 300px;
  }
`

const UnitRowLink = styled.span<{
  onClick?: () => void
}>`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  ${(p) =>
    p.onClick
      ? css`
          cursor: pointer;
          color: ${p.theme.colors.main.m1};
        `
      : ''}
`
