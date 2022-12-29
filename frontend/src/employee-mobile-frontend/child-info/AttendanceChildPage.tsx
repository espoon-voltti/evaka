// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { AttendanceStatus } from 'lib-common/generated/api-types/attendance'
import { useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
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
  childrenQuery,
  deleteChildImageMutation,
  returnToComingMutation
} from '../child-attendance/queries'
import { useAttendanceStatuses } from '../child-attendance/state'
import BottomModalMenu from '../common/BottomModalMenu'
import { FlexColumn } from '../common/components'
import { useTranslation } from '../common/i18n'
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

  const { unitId, childId, groupId } = useNonNullableParams<{
    unitId: UUID
    groupId: UUID | 'all'
    childId: UUID
  }>()

  const { unitInfoResponse } = useContext(UnitContext)
  const unitChildren = useQueryResult(childrenQuery(unitId))
  const childAttendanceStatuses = useAttendanceStatuses(unitId)

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

  const childInfoResult = useMemo(
    () =>
      combine(unitChildren, unitInfoResponse).map(([children, unitInfo]) => {
        const child = children.find((child) => child.id === childId)
        const group = child
          ? unitInfo.groups.find((group) => group.id === child.groupId)
          : undefined
        return { child, group }
      }),
    [unitChildren, unitInfoResponse, childId]
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
          combine(childInfoResult, childAttendanceStatuses),
          ([{ child, group }, childAttendanceStatuses]) => {
            if (!child) return null
            const childAttendance = childAttendanceStatuses.forChild(child.id)
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
                      unitId={unitId}
                      groupId={groupId}
                      child={child}
                    />
                  </Zindex>

                  <FlexColumn paddingHorizontal="s">
                    <AttendanceDailyServiceTimes
                      times={child.dailyServiceTimes}
                      reservations={child.reservations}
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
                    <Gap size="xs" />
                    {childAttendance.status === 'COMING' && (
                      <AttendanceChildComing
                        unitId={unitId}
                        child={child}
                        groupIdOrAll={groupId}
                      />
                    )}
                    {childAttendance.status === 'PRESENT' && (
                      <AttendanceChildPresent
                        child={child}
                        unitId={unitId}
                        groupIdOrAll={groupId}
                      />
                    )}
                    {childAttendance.status === 'DEPARTED' && (
                      <AttendanceChildDeparted
                        child={child}
                        unitId={unitId}
                        groupIdOrAll={groupId}
                      />
                    )}
                    {childAttendance.status === 'ABSENT' && (
                      <AttendanceChildAbsent
                        childId={child.id}
                        unitId={unitId}
                      />
                    )}
                  </FlexColumn>
                </Shadow>
                <BottomButtonWrapper>
                  <LinkButtonWithIcon
                    data-qa="mark-absent-beforehand"
                    to={`/units/${unitId}/groups/${groupId}/child-attendance/${childId}/mark-absent-beforehand`}
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
              {childInfoResult.isSuccess &&
                !!childInfoResult.value.child?.imageUrl && (
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
              deleteChildImage({ unitId, childId }).finally(() => {
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
              returnToComing({ unitId, childId }).finally(() => {
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
