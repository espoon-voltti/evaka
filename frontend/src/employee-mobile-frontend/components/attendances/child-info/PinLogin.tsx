import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'
import ReactSelect from 'react-select'

import {
  ChildResult,
  getChildSensitiveInformation,
  getDaycareAttendances
} from '../../../api/attendances'
import { Result } from '../../../../lib-common/api'
import { useTranslation } from '../../../state/i18n'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import Loader from '../../../../lib-components/atoms/Loader'
import InputField, {
  InputInfo
} from '../../../../lib-components/atoms/form/InputField'
import IconButton from '../../../../lib-components/atoms/buttons/IconButton'
import { ContentAreaWithShadow, TallContentArea } from '../../mobile/components'
import ErrorSegment from '../../../../lib-components/atoms/state/ErrorSegment'
import Title from '../../../../lib-components/atoms/Title'
import InlineButton from '../../../../lib-components/atoms/buttons/InlineButton'
import { defaultMargins, Gap } from '../../../../lib-components/white-space'
import colors from '../../../../lib-components/colors'
import { faArrowLeft, faArrowRight, faUserUnlock } from '../../../../lib-icons'
import ChildSensitiveInfo from './ChildSensitiveInfo'

export default React.memo(function PinLogin() {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [selectedStaff, setSelectedStaff] = useState<string>()
  const [selectedPin, setSelectedPin] = useState<string>('')
  const [childResult, setChildResult] = useState<Result<ChildResult>>()

  const { childId, unitId } = useParams<{
    unitId: string
    childId: string
  }>()

  const loadChildSensitiveInfo = () => {
    if (selectedStaff) {
      void getChildSensitiveInformation(
        childId,
        selectedStaff,
        selectedPin
      ).then(setChildResult)
    }
  }

  useEffect(() => {
    void getDaycareAttendances(unitId).then(setAttendanceResponse)
  }, [])

  const childBasicInfo =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const formatName = (firstName: string, lastName: string) =>
    `${lastName} ${firstName}`

  const getInputInfo = (): InputInfo | undefined => {
    return !childResult || !childResult.isSuccess
      ? undefined
      : {
          text: i18n.attendances.pin.status[childResult.value.status],
          status: 'warning'
        }
  }

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isFailure && <ErrorSegment />}
      {attendanceResponse.isSuccess && (
        <TallContentArea
          opaque={false}
          paddingHorizontal={'zero'}
          paddingVertical={'zero'}
        >
          <TopBarContainer>
            <BackButton
              onClick={() => history.goBack()}
              icon={faArrowLeft}
              text={
                childBasicInfo
                  ? `${childBasicInfo.firstName} ${childBasicInfo.lastName}`
                  : i18n.common.back
              }
            />
            <LogoutButtonWrapper>
              {childResult && (
                <IconButton
                  size={'L'}
                  icon={faUserUnlock}
                  onClick={() => {
                    setSelectedPin('')
                    history.goBack()
                  }}
                />
              )}
            </LogoutButtonWrapper>
          </TopBarContainer>
          {childResult &&
          childResult.isSuccess &&
          childResult.value.status === 'SUCCESS' ? (
            <ChildSensitiveInfo child={childResult.value.child} />
          ) : (
            <ContentAreaWithShadow
              opaque={true}
              paddingHorizontal={'s'}
              paddingVertical={'m'}
            >
              <FixedSpaceColumn alignItems={'center'} spacing={'m'}>
                <Title>{i18n.attendances.pin.header}</Title>
                <P>{i18n.attendances.pin.info}</P>
              </FixedSpaceColumn>
              <Gap />
              <FixedSpaceColumn spacing={'m'}>
                <Title>{i18n.attendances.pin.staff}</Title>
                <div data-qa={'select-staff'}>
                  <ReactSelect
                    placeholder={i18n.attendances.pin.selectStaff}
                    options={attendanceResponse.value.unit.staff.map(
                      (staff) => ({
                        label: formatName(staff.firstName, staff.lastName),
                        value: staff.id
                      })
                    )}
                    noOptionsMessage={() => i18n.attendances.pin.noOptions}
                    onChange={(option) =>
                      setSelectedStaff(
                        option && 'value' in option ? option.value : undefined
                      )
                    }
                  />
                </div>
                {selectedStaff && (
                  <Label>
                    {i18n.attendances.pin.pinCode}
                    <FixedSpaceRow spacing={'m'} alignItems={'center'}>
                      <InputField
                        placeholder={i18n.attendances.pin.pinCode}
                        onChange={setSelectedPin}
                        value={selectedPin || ''}
                        info={getInputInfo()}
                        width="s"
                        type="password"
                        data-qa="set-pin"
                      />
                      {selectedPin && selectedPin.length >= 4 && (
                        <IconButton
                          icon={faArrowRight}
                          onClick={loadChildSensitiveInfo}
                          dataQa={'submit-pin'}
                        />
                      )}
                    </FixedSpaceRow>
                  </Label>
                )}
              </FixedSpaceColumn>
            </ContentAreaWithShadow>
          )}
        </TallContentArea>
      )}
    </>
  )
})

const TopBarContainer = styled.div`
  display: grid;
  grid-template-columns: auto 50px;
`

const BackButton = styled(InlineButton)`
  color: ${colors.blues.dark};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
`

const LogoutButtonWrapper = styled.div`
  width: 40px;
  margin-left: auto;
  margin-right: 40px;
  margin-top: 16px;
  margin-bottom: 16px;
`
