{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}
import React, {
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState
} from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
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
import PinLogout from './PinLogout'
import useInactivityTimeout from './InactivityTimeout'

export default React.memo(function PinLogin() {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [selectedStaff, setSelectedStaff] = useState<string>()
  const [selectedPin, setSelectedPin] = useState<string>('')
  const [childResult, setChildResult] = useState<Result<ChildResult>>()
  const [loggingOut, setLoggingOut] = useState<boolean>(false)

  const pinInputRef = useRef<HTMLInputElement>(null)

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

  useLayoutEffect(() => {
    if (selectedStaff && pinInputRef && pinInputRef.current) {
      pinInputRef.current.focus()
    }
  }, [selectedStaff])

  const childBasicInfo =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const formatName = (firstName: string, lastName: string) =>
    `${lastName} ${firstName}`

  const loggedInStaffName = (): string => {
    const loggedInStaff = attendanceResponse.isSuccess
      ? attendanceResponse.value.unit.staff.find(
          (staff) => staff.id === selectedStaff
        )
      : null
    return loggedInStaff
      ? formatName(loggedInStaff.firstName, loggedInStaff.lastName)
      : ''
  }

  const getInputInfo = (): InputInfo | undefined => {
    return !childResult || !childResult.isSuccess
      ? undefined
      : {
          text: i18n.attendances.pin.status[childResult.value.status],
          status: 'warning'
        }
  }

  const logout = () => {
    setSelectedPin('')
    history.goBack()
  }

  const cancelLogout = () => {
    setLoggingOut(false)
  }

  useInactivityTimeout(120 * 1000, logout)

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
            <BackButtonWrapper>
              <BackButton
                onClick={() => history.goBack()}
                icon={faArrowLeft}
                text={
                  childBasicInfo
                    ? `${childBasicInfo.firstName} ${childBasicInfo.lastName}`
                    : i18n.common.back
                }
              />
            </BackButtonWrapper>
            <LogoutButtonWrapper>
              {childResult && (
                <IconButton
                  size={'L'}
                  icon={faUserUnlock}
                  data-qa={'button-logout'}
                  onClick={() => {
                    setLoggingOut(true)
                  }}
                />
              )}
            </LogoutButtonWrapper>
          </TopBarContainer>
          {childResult &&
          childResult.isSuccess &&
          childResult.value.status === 'SUCCESS' ? (
            <>
              {loggingOut && (
                <PinLogout
                  loggedInStaffName={loggedInStaffName()}
                  logout={logout}
                  cancel={cancelLogout}
                />
              )}
              <ChildSensitiveInfo child={childResult.value.child} />
            </>
          ) : (
            <ContentAreaWithShadow
              opaque={true}
              paddingHorizontal={'s'}
              paddingVertical={'m'}
            >
              <FixedSpaceColumn alignItems={'center'} spacing={'m'}>
                <Title>{i18n.attendances.pin.header}</Title>
                <span>{i18n.attendances.pin.info}</span>
              </FixedSpaceColumn>
              <Gap />
              <FixedSpaceColumn spacing={'m'}>
                <Key>{i18n.attendances.pin.staff}</Key>
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
                  <>
                    <Key>{i18n.attendances.pin.pinCode}</Key>
                    <FixedSpaceRow spacing={'m'} alignItems={'center'}>
                      <InputField
                        placeholder={i18n.attendances.pin.pinCode}
                        onChange={setSelectedPin}
                        value={selectedPin || ''}
                        info={getInputInfo()}
                        width="s"
                        type="password"
                        inputRef={pinInputRef}
                        data-qa="set-pin"
                      />
                      {selectedPin && selectedPin.length >= 4 && (
                        <IconButton
                          icon={faArrowRight}
                          onClick={loadChildSensitiveInfo}
                          data-qa={'submit-pin'}
                        />
                      )}
                    </FixedSpaceRow>
                  </>
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

const BackButtonWrapper = styled.div`
  width: calc(100% - 50px);
`

const BackButton = styled(InlineButton)`
  color: ${colors.blues.dark};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
  text-overflow: ellipsis;

  & span {
    white-space: normal;
  }
`

const Key = styled.span`
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 4px;
`

const LogoutButtonWrapper = styled.div`
  width: 40px;
  margin-left: auto;
  margin-right: 40px;
  margin-top: 16px;
  margin-bottom: 16px;
`
