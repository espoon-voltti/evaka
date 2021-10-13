// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useRef, useState } from 'react'
import { Link, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import RoundIcon from 'lib-components/atoms/RoundIcon'
import { faArrowLeft, faCalendarTimes, faQuestion, farUser } from 'lib-icons'
import { fontWeights } from 'lib-components/typography'
import { AttendanceStatus } from 'lib-common/generated/api-types/attendance'
import { useTranslation } from '../../../state/i18n'
import Loader from 'lib-components/atoms/Loader'
import { StaticChip } from 'lib-components/atoms/Chip'
import { defaultMargins, Gap } from 'lib-components/white-space'

import AttendanceChildComing from './child-state-pages/AttendanceChildComing'
import AttendanceChildPresent from './child-state-pages/AttendanceChildPresent'
import AttendanceChildDeparted from './child-state-pages/AttendanceChildDeparted'
import AttendanceDailyServiceTimes from '../AttendanceDailyServiceTimes'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { FlexColumn } from '../components'
import AttendanceChildAbsent from './child-state-pages/AttendanceChildAbsent'
import { BackButton, TallContentArea } from '../../mobile/components'
import ChildButtons from './ChildButtons'
import ImageEditor from './ImageEditor'
import BottomModalMenu from '../../common/BottomModalMenu'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import colors from 'lib-customizations/common'
import { deleteChildImage } from '../../../api/childImages'
import { IconBox } from '../ChildListItem'
import Absences from '../Absences'
import ArrivalAndDeparture from './ArrivalAndDeparture'
import { UnitContext } from '../../../state/unit'
import { UUID } from 'lib-common/types'

const ChildStatus = styled.div`
  color: ${colors.greyscale.medium};
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
  color: ${colors.blues.dark};
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
  color: ${colors.blues.dark};
  letter-spacing: 0.05rem;
`

const Zindex = styled.div`
  z-index: 1;
  margin-left: -8%;
  margin-right: -8%;
`

const ChildBackground = styled.div<{ status: AttendanceStatus }>`
  background: ${(p) => p.status && getBackgroundColorByStatus(p.status)};
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
  background: ${colors.greyscale.lightest};
`
const LinkButtonWithIcon = styled(Link)``

const LinkButtonText = styled.span`
  color: ${colors.blues.primary};
  margin-left: ${defaultMargins.s};
  font-weight: ${fontWeights.semibold};
  font-size: 16px;
  line-height: 16px;
`

const Shadow = styled.div`
  box-shadow: 0 4px 4px 0 ${colors.greyscale.lighter};
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: calc(100vh - 74px);
`

export default React.memo(function AttendanceChildPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, childId, groupId } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
    childId: UUID
  }>()

  const { unitInfoResponse } = useContext(UnitContext)

  const { attendanceResponse, reloadAttendances } = useContext(
    ChildAttendanceContext
  )

  useEffect(() => reloadAttendances(true), [reloadAttendances])

  const [uiMode, setUiMode] = useState<
    'default' | 'img-modal' | 'img-crop' | 'img-delete'
  >('default')

  const [rawImage, setRawImage] = useState<string | null>(null)

  const uploadInputRef = useRef<HTMLInputElement>(null)

  const loading = attendanceResponse.isLoading

  const child = attendanceResponse.isSuccess
    ? attendanceResponse.value.children.find((child) => child.id === childId)
    : undefined

  const group =
    attendanceResponse.isSuccess && unitInfoResponse.isSuccess
      ? unitInfoResponse.value.groups.find(
          (group) => group.id === child?.groupId
        )
      : undefined

  const groupNote = attendanceResponse.isSuccess
    ? attendanceResponse.value.groupNotes.find(
        (g) => g.groupId === child?.groupId
      )?.dailyNote
    : undefined

  if (uiMode === 'img-crop' && rawImage) {
    return (
      <ImageEditor
        image={rawImage}
        childId={childId}
        onReturn={() => {
          reloadAttendances()
          setRawImage(null)
          setUiMode('default')
        }}
      />
    )
  }

  return (
    <Fragment>
      <TallContentAreaNoOverflow
        opaque
        paddingHorizontal={'0px'}
        paddingVertical={'0px'}
        shadow
      >
        <BackButtonMargin
          onClick={() => history.goBack()}
          icon={faArrowLeft}
          data-qa="back-btn"
        />
        {child && group && !loading ? (
          <>
            <Shadow>
              <Zindex>
                <ChildBackground status={child.status}>
                  <Center>
                    <IconBox
                      type={child.status}
                      onClick={() => setUiMode('img-modal')}
                    >
                      {child.imageUrl ? (
                        <RoundImage src={child.imageUrl} />
                      ) : (
                        <RoundIcon
                          content={farUser}
                          color={getColorByStatus(child.status)}
                          size="XXL"
                        />
                      )}
                    </IconBox>

                    <Gap size={'s'} />

                    <CustomTitle data-qa={'child-name'}>
                      {child.firstName} {child.lastName}
                    </CustomTitle>

                    {child.preferredName && (
                      <CustomTitle data-qa={'child-preferred-name'}>
                        ({child.preferredName})
                      </CustomTitle>
                    )}

                    <GroupName>{group.name}</GroupName>

                    <ChildStatus>
                      <StaticChip
                        color={getColorByStatus(child.status)}
                        data-qa="child-status"
                      >
                        {i18n.attendances.types[child.status]}
                      </StaticChip>
                    </ChildStatus>
                  </Center>
                </ChildBackground>

                <ChildButtons
                  unitId={unitId}
                  groupId={groupId}
                  child={child}
                  groupNote={groupNote}
                />
              </Zindex>

              <FlexColumn paddingHorizontal={'s'}>
                <AttendanceDailyServiceTimes
                  times={child.dailyServiceTimes}
                  reservations={child.reservations}
                />
                <ArrivalAndDeparture child={child} />
                <Absences child={child} />
                <Gap size="xs" />
                {child.status === 'COMING' && (
                  <AttendanceChildComing
                    unitId={unitId}
                    child={child}
                    groupIdOrAll={groupId}
                  />
                )}
                {child.status === 'PRESENT' && (
                  <AttendanceChildPresent
                    child={child}
                    unitId={unitId}
                    groupIdOrAll={groupId}
                  />
                )}
                {child.status === 'DEPARTED' && (
                  <AttendanceChildDeparted child={child} unitId={unitId} />
                )}
                {child.status === 'ABSENT' && (
                  <AttendanceChildAbsent child={child} unitId={unitId} />
                )}
              </FlexColumn>
            </Shadow>
            <BottomButtonWrapper>
              <LinkButtonWithIcon
                data-qa="mark-absent-beforehand"
                to={`/units/${unitId}/groups/${groupId}/child-attendance/${childId}/mark-absent-beforehand`}
              >
                <RoundIcon
                  size={'L'}
                  content={faCalendarTimes}
                  color={colors.blues.primary}
                />
                <LinkButtonText>
                  {i18n.attendances.actions.markAbsentBeforehand}
                </LinkButtonText>
              </LinkButtonWithIcon>
            </BottomButtonWrapper>
          </>
        ) : (
          <Loader />
        )}
      </TallContentAreaNoOverflow>
      {uiMode === 'img-modal' && (
        <>
          <BottomModalMenu
            title={i18n.attendances.childInfo.image.modalMenu.title}
            onClose={() => setUiMode('default')}
          >
            <FixedSpaceColumn>
              <Button
                text={
                  i18n.attendances.childInfo.image.modalMenu.takeImageButton
                }
                primary
                onClick={() => {
                  if (uploadInputRef.current) uploadInputRef.current.click()
                }}
              />
              {child?.imageUrl && (
                <Button
                  text={
                    i18n.attendances.childInfo.image.modalMenu.deleteImageButton
                  }
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
          iconColour="orange"
          title={i18n.attendances.childInfo.image.modalMenu.deleteConfirm.title}
          resolve={{
            label:
              i18n.attendances.childInfo.image.modalMenu.deleteConfirm.resolve,
            action: () => {
              void deleteChildImage(childId).then((res) => {
                if (res.isFailure) {
                  console.error('Deleting image failed', res.message)
                } else {
                  reloadAttendances()
                  setUiMode('default')
                }
              })
            }
          }}
          reject={{
            label:
              i18n.attendances.childInfo.image.modalMenu.deleteConfirm.reject,
            action: () => setUiMode('default')
          }}
        />
      )}
    </Fragment>
  )
})

function getColorByStatus(status: AttendanceStatus) {
  switch (status) {
    case 'COMING':
      return colors.accents.water
    case 'PRESENT':
      return colors.accents.green
    case 'DEPARTED':
      return colors.blues.medium
    case 'ABSENT':
      return colors.greyscale.dark
  }
}

function getBackgroundColorByStatus(status: AttendanceStatus) {
  switch (status) {
    case 'COMING':
      return '#E2ECF2'
    case 'PRESENT':
      return '#EEF4B3'
    case 'DEPARTED':
      return '#B3C5DD'
    case 'ABSENT':
      return '#E8E8E8'
  }
}
