// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { AttendanceStatus } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQuery, useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import { StaticChip } from 'lib-components/atoms/Chip'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors, { attendanceColors } from 'lib-customizations/common'
import { faArrowLeft, faCalendarTimes, faQuestion, farUser } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { IconBox } from '../child-attendance/ChildListItem'
import {
  attendanceStatusesQuery,
  childrenQuery,
  deleteChildImageMutation,
  returnToComingMutation
} from '../child-attendance/queries'
import { childAttendanceStatus, useChild } from '../child-attendance/utils'
import { groupNotesQuery } from '../child-notes/queries'
import BottomModalMenu from '../common/BottomModalMenu'
import { FlexColumn } from '../common/components'
import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'
import { BackButton, TallContentArea } from '../pairing/components'

import Absences from './Absences'
import ArrivalAndDeparture from './ArrivalAndDeparture'
import AttendanceDailyServiceTimes from './AttendanceDailyServiceTimes'
import ChildButtons from './ChildButtons'
import ImageEditor from './ImageEditor'
import AttendanceChildAbsent from './child-state-pages/AttendanceChildAbsent'
import AttendanceChildComing from './child-state-pages/AttendanceChildComing'
import AttendanceChildDeparted from './child-state-pages/AttendanceChildDeparted'
import AttendanceChildPresent from './child-state-pages/AttendanceChildPresent'

export default React.memo(function AttendanceChildPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { unitId, childId } = useRequiredParams('unitId', 'childId')

  const { unitInfoResponse } = useContext(UnitContext)
  const { selectedGroupId, groupRoute } = useSelectedGroup()
  const { data: groupNotes } = useQuery(
    groupNotesQuery(selectedGroupId.type === 'all' ? '' : selectedGroupId.id),
    {
      enabled: selectedGroupId.type !== 'all'
    }
  )
  const groupHasNotes =
    selectedGroupId.type === 'all'
      ? false
      : !!(groupNotes && groupNotes.length > 1)
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId))

  const [uiMode, setUiMode] = useState<
    | 'default'
    | 'img-modal'
    | 'img-crop'
    | 'img-delete'
    | 'attendance-change-cancel'
  >('default')

  const [rawImage, setRawImage] = useState<string | null>(null)

  const uploadInputRef = useRef<HTMLInputElement>(null)

  const { mutateAsync: returnToComing } = useMutation(returnToComingMutation)
  const { mutateAsync: deleteChildImage } = useMutation(
    deleteChildImageMutation
  )

  const group = useMemo(
    () =>
      combine(child, unitInfoResponse).map(([child, unitInfo]) =>
        unitInfo.groups.find((group) => group.id === child.groupId)
      ),
    [child, unitInfoResponse]
  )

  const returnToComingModal = () => setUiMode('attendance-change-cancel')

  if (uiMode === 'img-crop' && rawImage) {
    return (
      <ImageEditor
        image={rawImage}
        unitId={unitId}
        childId={childId}
        onReturn={() => {
          setRawImage(null)
          setUiMode('default')
        }}
      />
    )
  }

  return (
    <>
      <TallContentAreaNoOverflow
        opaque
        paddingHorizontal="0px"
        paddingVertical="0px"
        shadow
      >
        <BackButtonMargin
          onClick={() => navigate(-1)}
          icon={faArrowLeft}
          data-qa="back-btn"
          aria-label={i18n.common.back}
        />
        {renderResult(
          combine(child, group, attendanceStatuses),
          ([child, group, attendanceStatuses]) => {
            if (!child) return null
            const childAttendance = childAttendanceStatus(
              child,
              attendanceStatuses
            )
            const today = LocalDate.todayInSystemTz()
            const childAge = today.differenceInYears(child.dateOfBirth)
            return (
              <>
                <Shadow>
                  <Zindex>
                    <ChildBackground status={childAttendance.status}>
                      <Center>
                        <IconBox
                          type={childAttendance.status}
                          onClick={() => setUiMode('img-modal')}
                        >
                          {child.imageUrl ? (
                            <RoundImage src={child.imageUrl} />
                          ) : (
                            <RoundIcon
                              content={farUser}
                              color={attendanceColors[childAttendance.status]}
                              size="XXL"
                            />
                          )}
                          <IconPlacementBox>
                            <RoundIconOnTop
                              content={`${childAge}v`}
                              color={
                                childAge < 3
                                  ? colors.accents.a6turquoise
                                  : colors.main.m1
                              }
                              size="L"
                            />
                          </IconPlacementBox>
                        </IconBox>

                        <Gap size="s" />
                        <CustomTitle data-qa="child-name">
                          {child.firstName} {child.lastName}
                        </CustomTitle>

                        {!!child.preferredName && (
                          <CustomTitle data-qa="child-preferred-name">
                            ({child.preferredName})
                          </CustomTitle>
                        )}

                        <GroupName>
                          {group?.name ?? i18n.attendances.noGroup}
                        </GroupName>

                        <ChildStatus>
                          <StaticChip
                            color={attendanceColors[childAttendance.status]}
                            data-qa="child-status"
                          >
                            {i18n.attendances.types[childAttendance.status]}
                          </StaticChip>
                        </ChildStatus>
                      </Center>
                    </ChildBackground>

                    <ChildButtons
                      groupHasNotes={groupHasNotes}
                      groupRoute={groupRoute}
                      child={child}
                    />
                  </Zindex>

                  <FlexColumn paddingHorizontal="s">
                    {child.scheduleType !== 'TERM_BREAK' ? (
                      <>
                        <AttendanceDailyServiceTimes
                          dailyServiceTimes={child.dailyServiceTimes}
                          reservations={child.reservations}
                          scheduleType={child.scheduleType}
                        />
                        <ArrivalAndDeparture
                          attendances={childAttendance.attendances}
                          returnToComing={returnToComingModal}
                        />
                        <Gap size="m" />
                        <Absences
                          absences={childAttendance.absences}
                          placementType={child.placementType}
                        />
                      </>
                    ) : (
                      <>
                        <Center data-qa="term-break">
                          {i18n.attendances.termBreak}
                        </Center>
                        <Gap size="XXL" />
                      </>
                    )}
                    <Gap size="xs" />
                    {childAttendance.status === 'COMING' && (
                      <AttendanceChildComing
                        child={child}
                        groupRoute={groupRoute}
                        attendances={childAttendance.attendances}
                      />
                    )}
                    {childAttendance.status === 'PRESENT' && (
                      <AttendanceChildPresent
                        child={child}
                        groupRoute={groupRoute}
                      />
                    )}
                    {childAttendance.status === 'DEPARTED' && (
                      <AttendanceChildDeparted
                        child={child}
                        groupRoute={groupRoute}
                      />
                    )}
                    {childAttendance.status === 'ABSENT' &&
                      child.scheduleType !== 'TERM_BREAK' && (
                        <AttendanceChildAbsent
                          childId={childId}
                          unitId={unitId}
                        />
                      )}
                  </FlexColumn>
                </Shadow>
                <BottomButtonWrapper>
                  {unitInfoResponse
                    .map((unit) => unit.features.includes('RESERVATIONS'))
                    .getOrElse(false) ? (
                    <LinkButtonWithIcon
                      data-qa="mark-reservations"
                      to={`${groupRoute}/child-attendance/${childId}/mark-reservations`}
                    >
                      <RoundIcon
                        size="L"
                        content={faCalendarTimes}
                        color={colors.main.m2}
                      />
                      <LinkButtonText>
                        {i18n.attendances.actions.markReservations}
                      </LinkButtonText>
                    </LinkButtonWithIcon>
                  ) : (
                    <LinkButtonWithIcon
                      data-qa="mark-absent-beforehand"
                      to={`${groupRoute}/child-attendance/${childId}/mark-absent-beforehand`}
                    >
                      <RoundIcon
                        size="L"
                        content={faCalendarTimes}
                        color={colors.main.m2}
                      />
                      <LinkButtonText>
                        {i18n.attendances.actions.markAbsentBeforehand}
                      </LinkButtonText>
                    </LinkButtonWithIcon>
                  )}
                </BottomButtonWrapper>
              </>
            )
          }
        )}
      </TallContentAreaNoOverflow>
      {uiMode === 'img-modal' && (
        <>
          <BottomModalMenu
            title={i18n.childInfo.image.modalMenu.title}
            onClose={() => setUiMode('default')}
          >
            <FixedSpaceColumn>
              <Button
                text={i18n.childInfo.image.modalMenu.takeImageButton}
                primary
                onClick={() => {
                  if (uploadInputRef.current) uploadInputRef.current.click()
                }}
              />
              {child.isSuccess && !!child.value.imageUrl && (
                <Button
                  text={i18n.childInfo.image.modalMenu.deleteImageButton}
                  onClick={() => setUiMode('img-delete')}
                />
              )}
            </FixedSpaceColumn>
          </BottomModalMenu>
          <input
            ref={uploadInputRef}
            style={{ display: 'hidden' }}
            type="file"
            accept="image/jpeg, image/png"
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
              if (
                event.target &&
                event.target.files &&
                event.target.files.length > 0
              ) {
                const reader = new FileReader()
                reader.addEventListener('load', () => {
                  if (typeof reader.result === 'string') {
                    setRawImage(reader.result)
                    setUiMode('img-crop')
                  }
                })
                reader.readAsDataURL(event.target.files[0])
              }
            }}
          />
        </>
      )}
      {uiMode == 'img-delete' && (
        <InfoModal
          icon={faQuestion}
          type="warning"
          title={i18n.childInfo.image.modalMenu.deleteConfirm.title}
          resolve={{
            label: i18n.childInfo.image.modalMenu.deleteConfirm.resolve,
            action: () => {
              void deleteChildImage({ unitId, childId }).finally(() => {
                setUiMode('default')
              })
            }
          }}
          reject={{
            label: i18n.childInfo.image.modalMenu.deleteConfirm.reject,
            action: () => setUiMode('default')
          }}
        />
      )}
      {uiMode == 'attendance-change-cancel' && (
        <InfoModal
          icon={faQuestion}
          type="warning"
          title={i18n.attendances.confirmAttendanceChangeCancel}
          resolve={{
            label: i18n.common.yesIDo,
            action: () => {
              void returnToComing({ unitId, childId }).finally(() => {
                setUiMode('default')
                navigate(-1)
              })
            }
          }}
          reject={{
            label: i18n.common.noIDoNot,
            action: () => setUiMode('default')
          }}
        />
      )}
    </>
  )
})

const ChildStatus = styled.div`
  color: ${colors.grayscale.g35};
  top: 10px;
  position: relative;
`

const RoundImage = styled.img`
  display: block;
  border-radius: 100%;
  width: 128px;
  height: 128px;
`

const CustomTitle = styled.h2`
  font-family: Montserrat, 'Arial', sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 20px;
  line-height: 30px;
  margin-top: 0;
  color: ${colors.main.m1};
  text-align: center;
  margin-bottom: ${defaultMargins.xs};
`

const GroupName = styled.div`
  font-family: 'Open Sans', 'Arial', sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 15px;
  line-height: 22px;
  text-transform: uppercase;
  color: ${colors.main.m1};
  letter-spacing: 0.05rem;
`

const Zindex = styled.div`
  z-index: 1;
  margin-left: -8%;
  margin-right: -8%;
`

const ChildBackground = styled.div<{ status: AttendanceStatus }>`
  background-color: ${(p) =>
    attendanceColors[p.status]}48; // hex 48 is 0.3 alpha
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 0 0 50% 50%;
  padding-top: ${defaultMargins.s};
`

const BackButtonMargin = styled(BackButton)`
  margin-left: 8px;
  margin-top: 8px;
  z-index: 2;
`

export const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`

const Center = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100vw;
`

const BottomButtonWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 74px;
  background: ${colors.grayscale.g4};
`
const LinkButtonWithIcon = styled(Link)``

const LinkButtonText = styled.span`
  color: ${colors.main.m2};
  margin-left: ${defaultMargins.s};
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  line-height: 16px;
`

const Shadow = styled.div`
  box-shadow: 0 4px 4px 0 ${colors.grayscale.g15};
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: calc(100vh - 74px);
`
const RoundIconOnTop = styled(RoundIcon)`
  position: absolute;
  left: 100px;
  top: -34px;
  z-index: 2;
`
const IconPlacementBox = styled.div`
  position: relative;
  width: 0;
  height: 0;
`
