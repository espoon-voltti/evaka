// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result, Success } from 'lib-common/api'
import { ChildResult, Staff } from 'lib-common/generated/api-types/attendance'
import { useRestApi } from 'lib-common/utils/useRestApi'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Combobox from 'lib-components/atoms/form/Combobox'
import { InputInfo } from 'lib-components/atoms/form/InputField'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faArrowLeft, faArrowRight, faUserUnlock } from 'lib-icons'
import { sortBy } from 'lodash'
import React, {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { PlainPinInput } from 'lib-components/molecules/PinInput'
import { getChildSensitiveInformation } from '../../../api/attendances'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { UnitContext } from '../../../state/unit'
import { renderResult, UnwrapResult } from '../../async-rendering'
import { TallContentArea } from '../../mobile/components'
import { BackButtonInline } from '../components'
import ChildSensitiveInfo from './ChildSensitiveInfo'
import useInactivityTimeout from './InactivityTimeout'
import PinLogout from './PinLogout'

export default React.memo(function PinLogin() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(reloadUnitInfo, [reloadUnitInfo])

  const { attendanceResponse } = useContext(ChildAttendanceContext)

  const { childId, groupId } = useParams<{
    groupId: string
    childId: string
  }>()

  const [selectedStaff, setSelectedStaff] = useState<{
    name: string
    id: string
  }>()
  const [pinCode, setPinCode] = useState('')
  const [loggingOut, setLoggingOut] = useState<boolean>(false)

  const [childSensitiveResult, setChildSensitiveResult] = useState<
    Result<ChildResult | undefined>
  >(Success.of(undefined))

  const loadChildSensitiveInfo = useRestApi(
    getChildSensitiveInformation,
    setChildSensitiveResult
  )

  const fetchChildSensitiveInfo = useCallback(() => {
    if (selectedStaff) {
      loadChildSensitiveInfo(childId, selectedStaff.id, pinCode)
    }
  }, [childId, loadChildSensitiveInfo, pinCode, selectedStaff])

  const pinInputRef = useRef<HTMLInputElement>(null)
  useLayoutEffect(() => {
    if (selectedStaff) {
      pinInputRef?.current?.focus()
    }
  }, [selectedStaff])

  const formatName = ({ firstName, lastName }: Staff) =>
    `${lastName} ${firstName}`

  const staffOptions = useMemo(
    () =>
      sortBy(
        unitInfoResponse
          .map(({ staff }) => staff.filter(({ pinSet }) => pinSet))
          .getOrElse([]),
        ({ groups }) => (groups.includes(groupId) ? 0 : 1),
        ({ lastName }) => lastName,
        ({ firstName }) => firstName
      ).map((staff) => ({
        name: formatName(staff),
        id: staff.id
      })),
    [groupId, unitInfoResponse]
  )

  const loggedInStaffName = useMemo(
    (): string =>
      unitInfoResponse
        .map(({ staff }) => staff.find((s) => s.id === selectedStaff?.id))
        .map((staff) => (staff ? formatName(staff) : ''))
        .getOrElse(''),
    [selectedStaff, unitInfoResponse]
  )

  const inputInfo = useMemo(
    () =>
      childSensitiveResult
        .map<InputInfo | undefined>((value) =>
          value
            ? {
                text: i18n.attendances.pin.status[value.status],
                status: 'warning'
              }
            : undefined
        )
        .getOrElse(undefined),
    [childSensitiveResult, i18n.attendances.pin.status]
  )

  const logout = useCallback(() => {
    setPinCode('')
    history.goBack()
  }, [history])

  const cancelLogout = useCallback(() => setLoggingOut(false), [])

  useInactivityTimeout(120 * 1000, logout)

  const basicChild = useMemo(
    () =>
      attendanceResponse.map((attendance) =>
        attendance.children.find((ac) => ac.id === childId)
      ),
    [attendanceResponse, childId]
  )

  return (
    <TallContentAreaNoOverflow
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <UnwrapResult result={basicChild}>
        {(child) => (
          <>
            <TopBarContainer>
              <TopRow>
                <BackButtonInline
                  onClick={() => history.goBack()}
                  icon={faArrowLeft}
                  text={
                    child
                      ? `${child.firstName} ${child.lastName}`
                      : i18n.common.back
                  }
                  data-qa="go-back"
                />
                {childSensitiveResult.isSuccess &&
                  childSensitiveResult.value?.child && (
                    <IconButton
                      size="L"
                      icon={faUserUnlock}
                      data-qa="button-logout"
                      onClick={() => setLoggingOut(true)}
                    />
                  )}
              </TopRow>
            </TopBarContainer>
            {renderResult(childSensitiveResult, (childSensitiveInfo) =>
              childSensitiveInfo?.status === 'SUCCESS' ? (
                <>
                  {loggingOut && (
                    <PinLogout
                      loggedInStaffName={loggedInStaffName}
                      logout={logout}
                      cancel={cancelLogout}
                    />
                  )}
                  <ChildSensitiveInfo child={childSensitiveInfo.child} />
                </>
              ) : (
                <ContentArea
                  shadow
                  opaque
                  paddingHorizontal="s"
                  paddingVertical="m"
                >
                  <FixedSpaceColumn alignItems="center" spacing="m">
                    <Title>{i18n.attendances.pin.header}</Title>
                    <span>{i18n.attendances.pin.info}</span>
                  </FixedSpaceColumn>
                  <Gap />
                  <FixedSpaceColumn spacing="m">
                    <Key>{i18n.attendances.pin.staff}</Key>
                    <div data-qa="select-staff">
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
                        <FixedSpaceRow spacing="m" alignItems="center">
                          <PlainPinInput
                            onChange={setPinCode}
                            pin={pinCode}
                            info={inputInfo}
                            inputRef={pinInputRef}
                          />
                          {pinCode.length === 4 && (
                            <IconButton
                              icon={faArrowRight}
                              onClick={fetchChildSensitiveInfo}
                              data-qa="submit-pin"
                            />
                          )}
                        </FixedSpaceRow>
                      </>
                    )}
                  </FixedSpaceColumn>
                </ContentArea>
              )
            )}
          </>
        )}
      </UnwrapResult>
    </TallContentAreaNoOverflow>
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
  font-weight: ${fontWeights.semibold};
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
