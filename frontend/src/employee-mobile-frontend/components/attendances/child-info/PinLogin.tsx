// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { sortBy } from 'lodash'

import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ContentArea } from 'lib-components/layout/Container'
import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import InputField, { InputInfo } from 'lib-components/atoms/form/InputField'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Title from 'lib-components/atoms/Title'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Combobox from 'lib-components/atoms/form/Combobox'
import { faArrowLeft, faArrowRight, faUserUnlock } from 'lib-icons'

import { TallContentArea } from '../../mobile/components'

import {
  ChildResult,
  getChildSensitiveInformation,
  getDaycareAttendances
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import ChildSensitiveInfo from './ChildSensitiveInfo'
import PinLogout from './PinLogout'
import useInactivityTimeout from './InactivityTimeout'
import { BackButtonInline } from '../components'

export default React.memo(function PinLogin() {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { attendanceResponse, setAttendanceResponse } =
    useContext(AttendanceUIContext)

  const [selectedStaff, setSelectedStaff] = useState<{
    name: string
    id: string
  }>()
  const [selectedPin, setSelectedPin] = useState<string>('')
  const [childResult, setChildResult] = useState<Result<ChildResult>>()
  const [loggingOut, setLoggingOut] = useState<boolean>(false)

  const pinInputRef = useRef<HTMLInputElement>(null)

  const { childId, groupId, unitId } = useParams<{
    unitId: string
    groupId: string
    childId: string
  }>()

  const loadChildSensitiveInfo = () => {
    if (selectedStaff) {
      void getChildSensitiveInformation(
        childId,
        selectedStaff.id,
        selectedPin
      ).then(setChildResult)
    }
  }

  useEffect(() => {
    void getDaycareAttendances(unitId).then(setAttendanceResponse)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useLayoutEffect(() => {
    if (selectedStaff && pinInputRef && pinInputRef.current) {
      pinInputRef.current.focus()
    }
  }, [selectedStaff])

  function formatName(firstName: string, lastName: string) {
    return `${lastName} ${firstName}`
  }

  const staffOptions = useMemo(() => {
    if (attendanceResponse.isSuccess) {
      return sortBy(
        attendanceResponse.value.unit.staff.filter(({ pinSet }) => pinSet),
        ({ groups }) => (groups.includes(groupId) ? 0 : 1),
        ({ lastName }) => lastName,
        ({ firstName }) => firstName
      ).map((staff) => ({
        name: formatName(staff.firstName, staff.lastName),
        id: staff.id
      }))
    } else {
      return []
    }
  }, [groupId, attendanceResponse])

  const childBasicInfo =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const loggedInStaffName = (): string => {
    const loggedInStaff = attendanceResponse.isSuccess
      ? attendanceResponse.value.unit.staff.find(
          (staff) => staff.id === selectedStaff?.id
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
        <TallContentAreaNoOverflow
          opaque={false}
          paddingHorizontal={'zero'}
          paddingVertical={'zero'}
        >
          <TopBarContainer>
            <TopRow>
              <BackButtonInline
                onClick={() => history.goBack()}
                icon={faArrowLeft}
                text={
                  childBasicInfo
                    ? `${childBasicInfo.firstName} ${childBasicInfo.lastName}`
                    : i18n.common.back
                }
                data-qa="go-back"
              />
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
            </TopRow>
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
            <ContentArea
              shadow
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
                  <Combobox
                    items={staffOptions}
                    selectedItem={selectedStaff ?? null}
                    onChange={(item) => setSelectedStaff(item ?? undefined)}
                    getItemLabel={({ name }) => name}
                    menuEmptyLabel={i18n.attendances.pin.noOptions}
                    placeholder={i18n.attendances.pin.selectStaff}
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
            </ContentArea>
          )}
        </TallContentAreaNoOverflow>
      )}
    </>
  )
})

const TopBarContainer = styled.div`
  display: grid;
  grid-template-columns: auto 50px;
`

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`

const Key = styled.span`
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 4px;
`

const TopRow = styled.div`
  display: flex;
  justify-content: space-between;
  max-width: 100vw;

  button {
    margin-right: ${defaultMargins.s};
  }
`
