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
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H3, Light } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faChevronDown, faChevronRight } from 'lib-icons'
import { faHeart } from 'lib-icons'
import { faEye } from 'lib-icons'
import {
  faArrowLeft,
  faCommentAlt,
  fasCommentAltLines,
  faSection
} from 'lib-icons'
import { faUndo } from 'lib-icons'
import { faCheck, faFile, faPen, faTimes } from 'lib-icons'

import { ApplicationUIContext } from '../../../state/application-ui'
import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import { placementTypeToCareTypeLabel } from '../../common/CareTypeLabel'
import {
  BasisFragment,
  DateOfBirthInfo,
  hasBasisIndicators,
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
    toggleApplicationExpanded,
    isApplicationExpanded,
    setSavedScrollPosition
  } = React.useContext(ApplicationUIContext)

  const {
    mutateAsync: upsertApplicationPlacementDraft,
    isPending: updatePending
  } = useMutation(upsertApplicationPlacementDraftMutation)

  const { isPending: deletePending } = useMutation(
    deleteApplicationPlacementDraftMutation
  )

  const [editingNote, setEditingNote] = useState(false)
  const [addingOtherUnit, setAddingOtherUnit] = useState(false)

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

  const expanded = isApplicationExpanded(application.id)

  const draftPlacementUnit = useMemo(
    () =>
      unitRows.find(
        (u) =>
          application.placementDraft &&
          application.placementDraft.unit.id === u.id
      ),
    [unitRows, application.placementDraft]
  )

  const showBasisIndicators = useMemo(
    () => hasBasisIndicators(application),
    [application]
  )

  return (
    <Card
      $placed={application.placementDraft !== null}
      data-qa="application-card"
    >
      {editingNote && (
        <ServiceWorkerNoteModal
          applicationId={application.id}
          serviceWorkerNote={application.serviceWorkerNote}
          onClose={() => setEditingNote(false)}
        />
      )}
      <HeaderContainer>
        <HeaderRow justifyContent="space-between" alignItems="center">
          <HeaderLeftContent>
            <ExpandToggle
              onClick={() => toggleApplicationExpanded(application.id)}
            >
              <IconOnlyButton
                icon={expanded ? faChevronDown : faChevronRight}
                aria-label={expanded ? i18n.common.close : i18n.common.open}
              />
              <ChildNameHeading
                noMargin
                $limitedWidth={!expanded && !!draftPlacementUnit}
                data-qa="child-name"
              >
                {application.lastName} {application.firstName}
              </ChildNameHeading>
            </ExpandToggle>
            {!expanded && draftPlacementUnit && (
              <CollapsedPlacementInline>
                <UnitRow
                  application={application}
                  unit={draftPlacementUnit}
                  unitVisible={shownDaycares.some(
                    (d) => d.id === draftPlacementUnit.id
                  )}
                  lastRow
                  readonly
                  onUpsertApplicationPlacementSuccess={() => undefined}
                  onDeleteApplicationPlacementSuccess={() => undefined}
                  onMutateApplicationPlacementFailure={() => undefined}
                  onAddOrHighlightDaycare={onAddOrHighlightDaycare}
                />
              </CollapsedPlacementInline>
            )}
          </HeaderLeftContent>
          <HeaderRightContent>
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
            <a
              href={`/employee/applications/${application.id}`}
              target="_blank"
              rel="noreferrer"
            >
              <IconOnlyButton
                icon={faFile}
                aria-label={i18n.common.open}
                data-qa="open-application-button"
              />
            </a>
            {application.checkedByAdmin ? (
              <Button
                appearance="button"
                text={i18n.applications.placementDesktop.toPlacementPlan}
                data-qa="to-placement-plan-button"
                onClick={() => {
                  // Save current scroll position before navigating
                  setSavedScrollPosition(window.scrollY)
                  navigate(`/applications/${application.id}/placement`)
                }}
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
                  primary
                />
              </a>
            )}
          </HeaderRightContent>
        </HeaderRow>
        {!expanded && draftPlacementUnit && (
          <CollapsedPlacementSeparateRow>
            <UnitRow
              application={application}
              unit={draftPlacementUnit}
              unitVisible={shownDaycares.some(
                (d) => d.id === draftPlacementUnit.id
              )}
              lastRow
              readonly
              onUpsertApplicationPlacementSuccess={() => undefined}
              onDeleteApplicationPlacementSuccess={() => undefined}
              onMutateApplicationPlacementFailure={() => undefined}
              onAddOrHighlightDaycare={onAddOrHighlightDaycare}
            />
          </CollapsedPlacementSeparateRow>
        )}
      </HeaderContainer>
      {expanded && (
        <ExpandedContentWrapper>
          <DatesAndBasisArea>
            <DatesGrid>
              <GridItem>
                <FixedSpaceRow spacing="xs" alignItems="center">
                  <PlacementCircle
                    type={
                      isPartDayPlacement(application.placementType)
                        ? 'half'
                        : 'full'
                    }
                    label={
                      application.serviceNeed !== null
                        ? application.serviceNeed.nameFi
                        : i18n.placement.type[application.placementType]
                    }
                    size={24}
                  />
                  <span>
                    {
                      i18n.common.careTypeLabelsShort[
                        placementTypeToCareTypeLabel(application.placementType)
                      ]
                    }
                  </span>
                </FixedSpaceRow>
              </GridItem>
              <GridItem>
                <Tooltip
                  tooltip={i18n.applications.placementDesktop.birthDate}
                  delayed
                >
                  <DateOfBirthInfo application={application} />
                </Tooltip>
              </GridItem>
              <GridItem>
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
                    <div data-qa="due-date">
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
              </GridItem>
              <GridItem>
                <Tooltip
                  tooltip={
                    i18n.applications.placementDesktop.preferredStartDate
                  }
                  delayed
                >
                  <FixedSpaceRow spacing="xs" alignItems="center">
                    <RoundIcon
                      content={faHeart}
                      color={colors.grayscale.g15}
                      textColor={colors.grayscale.g100}
                      size="m"
                    />
                    <div data-qa="preferred-start-date">
                      {application.startDate?.format() ?? '-'}
                    </div>
                  </FixedSpaceRow>
                </Tooltip>
              </GridItem>
            </DatesGrid>
            {showBasisIndicators && (
              <BasisWrapper>
                <BasisFragment application={application} />
              </BasisWrapper>
            )}
          </DatesAndBasisArea>
          <FixedSpaceColumn spacing="zero" style={{ flexGrow: 1, minWidth: 0 }}>
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
            <div style={{ padding: defaultMargins.xs }}>
              {addingOtherUnit ? (
                <FixedSpaceRow spacing="XL" justifyContent="space-between">
                  <Combobox
                    data-qa="draft-placement-combobox"
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
                          .then(({ startDate }) => {
                            onUpsertApplicationPlacementSuccess(
                              application.id,
                              unit,
                              startDate
                            )
                            setAddingOtherUnit(false)
                          })
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
                  <Button
                    appearance="inline"
                    text={i18n.common.cancel}
                    onClick={() => {
                      setAddingOtherUnit(false)
                    }}
                    data-qa="cancel-add-other-unit-button"
                  />
                </FixedSpaceRow>
              ) : (
                <Button
                  appearance="inline"
                  text={i18n.applications.placementDesktop.addToOtherUnit}
                  onClick={() => {
                    setAddingOtherUnit(true)
                  }}
                  data-qa="add-other-unit-button"
                />
              )}
            </div>
          </FixedSpaceColumn>
        </ExpandedContentWrapper>
      )}
    </Card>
  )
})

const UnitRow = React.memo(function UnitRow({
  application,
  unit,
  unitVisible,
  lastRow,
  readonly,
  onUpsertApplicationPlacementSuccess,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onAddOrHighlightDaycare
}: {
  application: ApplicationSummary
  unit: PreferredUnit
  unitVisible: boolean
  lastRow: boolean
  readonly?: boolean
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
      data-qa="unit-preference"
    >
      <UnitRowLink
        data-qa="unit-preference-title"
        onClick={
          unitVisible
            ? () => {
                onAddOrHighlightDaycare(unit)
              }
            : undefined
        }
      >
        {preferenceIndex >= 0
          ? `${preferenceIndex + 1}. ${unit.name}`
          : `${i18n.applications.placementDesktop.other}: ${unit.name}`}
      </UnitRowLink>

      {application.placementDraft &&
        placedHere &&
        (readonly ? (
          <PlacementDateSpan data-qa="placement-date">
            {application.placementDraft.startDate.format()} –
          </PlacementDateSpan>
        ) : (
          <PlacementControlsWrapper>
            {editingDate ? (
              <DateEditor
                applicationId={application.id}
                placementDraft={application.placementDraft}
                onUpsertApplicationPlacementSuccess={
                  onUpsertApplicationPlacementSuccess
                }
                onClose={() => setEditingDate(false)}
              />
            ) : (
              <PlacementDraftControls
                spacing="m"
                justifyContent="space-between"
              >
                <FixedSpaceRow spacing="s">
                  <PlacementDateSpan data-qa="placement-date">
                    {application.placementDraft.startDate.format()} –
                  </PlacementDateSpan>
                  <IconOnlyButton
                    data-qa="edit-placement-date-button"
                    icon={faPen}
                    onClick={() => {
                      setEditingDate(true)
                    }}
                    aria-label={i18n.common.edit}
                  />
                </FixedSpaceRow>
                <Tooltip
                  tooltip={
                    i18n.applications.placementDesktop.cancelPlacementDraft
                  }
                  delayed
                >
                  <span>
                    <MutateIconOnlyButton
                      icon={faUndo}
                      aria-label={
                        i18n.applications.placementDesktop.cancelPlacementDraft
                      }
                      data-qa="cancel-placement-draft-button"
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
                  </span>
                </Tooltip>
              </PlacementDraftControls>
            )}
          </PlacementControlsWrapper>
        ))}

      {!readonly &&
        application.placementDraft?.unit.id !== unit.id &&
        (unitVisible ? (
          <span>
            <MutateIconOnlyButton
              aria-label={
                i18n.applications.placementDesktop.createPlacementDraft
              }
              data-qa="create-placement-draft-button"
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
              onSuccess={({ startDate }) => {
                onAddOrHighlightDaycare(unit)
                onUpsertApplicationPlacementSuccess(
                  application.id,
                  unit,
                  startDate
                )
              }}
              onFailure={onMutateApplicationPlacementFailure}
              successTimeout={0}
            />
          </span>
        ) : (
          <span>
            <IconOnlyButton
              icon={faEye}
              aria-label={i18n.applications.placementDesktop.show}
              data-qa="show-unit-button"
              onClick={() => {
                onAddOrHighlightDaycare(unit)
              }}
            />
          </span>
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
        data-qa="placement-date-picker"
        date={date}
        onChange={(val) => {
          if (val) setDate(val)
        }}
        locale={lang}
      />
      <FixedSpaceRow spacing="xs" alignItems="center">
        <span>
          <MutateIconOnlyButton
            data-qa="save-placement-date-button"
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
        </span>
        <IconOnlyButton
          icon={faTimes}
          onClick={() => {
            onClose()
          }}
          aria-label={i18n.common.cancel}
        />
      </FixedSpaceRow>
    </FixedSpaceRow>
  )
})

const Card = styled.div<{ $placed: boolean }>`
  width: 100%;
  min-width: 630px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  border-left: 4px solid
    ${(p) =>
      p.$placed ? p.theme.colors.grayscale.g15 : p.theme.colors.main.m3};
  border-radius: 4px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const HeaderContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xs};
  padding: ${defaultMargins.s} ${defaultMargins.s} ${defaultMargins.s} 0;
`

const ExpandToggle = styled.div`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};
  cursor: pointer;
  flex: 1;
  min-width: 0;
  padding: ${defaultMargins.s} ${defaultMargins.xs} ${defaultMargins.s}
    ${defaultMargins.s};
  margin: -${defaultMargins.s} -${defaultMargins.xs} -${defaultMargins.s} 0;
`

const HeaderRow = styled(FixedSpaceRow)`
  @media (max-width: 1400px) {
    flex-direction: row;
    align-items: center !important;
  }
`

const HeaderLeftContent = styled(FixedSpaceRow).attrs({
  alignItems: 'center'
})`
  flex: 1;
  min-width: 0;
  margin-right: ${defaultMargins.s};
`

const HeaderRightContent = styled(FixedSpaceRow).attrs({
  alignItems: 'center'
})``

const CollapsedPlacementInline = styled.div`
  flex: 1;
  min-width: 0;
  overflow: hidden;

  @media (max-width: 1400px) {
    display: none;
  }
`

const CollapsedPlacementSeparateRow = styled.div`
  width: 100%;
  min-width: 0;
  overflow: hidden;
  display: none;

  @media (max-width: 1400px) {
    display: block;
  }
`

const breakpoint1600 = '1600px'
const breakpoint1300 = '1300px'

const ExpandedContentWrapper = styled.div`
  display: flex;
  flex-direction: row;
  min-width: 0;
  gap: ${defaultMargins.s};
  padding: 0 ${defaultMargins.s} ${defaultMargins.s} 42px;

  @media (max-width: ${breakpoint1300}) {
    flex-direction: column;
  }
`

const BasisWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: flex-start;
  gap: ${defaultMargins.s} ${defaultMargins.s};
`

const DatesAndBasisArea = styled(FixedSpaceColumn)`
  background-color: ${(p) => p.theme.colors.grayscale.g4};
  padding: ${defaultMargins.s};
  flex-shrink: 1;
  min-width: 0;
`

const DatesGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr;
  gap: ${defaultMargins.xs} ${defaultMargins.s};
  min-width: 0;

  @media (max-width: ${breakpoint1300}) {
    /* Below 1300px: horizontal row layout */
    grid-template-columns: repeat(4, 1fr);
    grid-auto-flow: column;
    gap: ${defaultMargins.xs};
  }

  @media (min-width: ${breakpoint1600}) {
    /* Above 1600px: 2x2 grid layout */
    grid-template-columns: 1fr 1fr;
  }

  > * {
    min-width: 0;
    overflow: hidden;
  }
`

const GridItem = styled.div`
  width: 133px;
  overflow: hidden;
`

const UnitRowContainer = styled(FixedSpaceRow)<{
  $placedHere: boolean
  $last: boolean
}>`
  width: 100%;
  min-width: 0;
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

const UnitRowLink = styled.span<{
  onClick?: (e: React.MouseEvent) => void
}>`
  margin-right: 36px;
  flex: 1 1 150px;
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

const PlacementControlsWrapper = styled.div`
  flex-shrink: 0;

  @media (min-width: ${breakpoint1300}) {
    width: 226px;
  }
`

const PlacementDraftControls = styled(FixedSpaceRow)`
  width: 100%;
  padding-left: 8px;
`

const PlacementDateSpan = styled.span`
  white-space: nowrap;
  flex-shrink: 0;
`

const ChildNameHeading = styled(H3)<{ $limitedWidth: boolean }>`
  color: ${(p) => p.theme.colors.main.m2};
  flex: ${(p) => (p.$limitedWidth ? 1 : 'none')};
  min-width: 0;
  max-width: ${(p) => (p.$limitedWidth ? '236px' : 'none')};
  word-wrap: break-word;
  white-space: normal;

  @media (max-width: 1400px) {
    flex: 1;
    max-width: none;
  }
`
