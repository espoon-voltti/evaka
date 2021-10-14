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
import { InputInfo } from 'lib-components/atoms/form/InputField'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import Title from 'lib-components/atoms/Title'
import { defaultMargins, Gap } from 'lib-components/white-space'
import Combobox from 'lib-components/atoms/form/Combobox'
import { faArrowLeft, faArrowRight, faUserUnlock } from 'lib-icons'
import { fontWeights } from 'lib-components/typography'
import { ChildResult, Staff } from 'lib-common/generated/api-types/attendance'
import { EMPTY_PIN, PinInput } from 'lib-components/molecules/PinInput'
import { renderResult } from '../../async-rendering'
import { TallContentArea } from '../../mobile/components'
import { getChildSensitiveInformation } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import ChildSensitiveInfo from './ChildSensitiveInfo'
import PinLogout from './PinLogout'
import useInactivityTimeout from './InactivityTimeout'
import { BackButtonInline } from '../components'
import { UnitContext } from '../../../state/unit'

export default React.memo(function PinLogin() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitInfoResponse, reloadUnitInfo } = useContext(UnitContext)
  useEffect(reloadUnitInfo, [reloadUnitInfo])

  const { attendanceResponse } = useContext(ChildAttendanceContext)

  const [selectedStaff, setSelectedStaff] = useState<{
    name: string
    id: string
  }>()
  const [selectedPin, setSelectedPin] = useState(EMPTY_PIN)
  const [childResult, setChildResult] = useState<Result<ChildResult>>()
  const [loggingOut, setLoggingOut] = useState<boolean>(false)

  const pinInputRef = useRef<HTMLInputElement>(null)
  useLayoutEffect(() => {
    if (selectedStaff) {
      pinInputRef?.current?.focus()
    }
  }, [selectedStaff])

  const { childId, groupId } = useParams<{
    groupId: string
    childId: string
  }>()

  const loadChildSensitiveInfo = () => {
    if (selectedStaff) {
      void getChildSensitiveInformation(
        childId,
        selectedStaff.id,
        selectedPin.join('')
      ).then(setChildResult)
    }
  }

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

  const loggedInStaffName = (): string =>
    unitInfoResponse
      .map(({ staff }) => staff.find((s) => s.id === selectedStaff?.id))
      .map((staff) => (staff ? formatName(staff) : ''))
      .getOrElse('')

  const getInputInfo = () =>
    childResult
      ?.map<InputInfo>((value) => ({
        text: i18n.attendances.pin.status[value.status],
        status: 'warning'
      }))
      .getOrElse(undefined)

  const logout = () => {
    setSelectedPin(EMPTY_PIN)
    history.goBack()
  }

  const cancelLogout = () => setLoggingOut(false)

  useInactivityTimeout(120 * 1000, logout)

  return renderResult(attendanceResponse, (attendance) => {
    const child = attendance.children.find((ac) => ac.id === childId)
    const childName = child ? `${child.firstName} ${child.lastName}` : null
    return (
      <>
        <TallContentAreaNoOverflow
          opaque={false}
          paddingHorizontal="zero"
          paddingVertical="zero"
        >
          <TopBarContainer>
            <TopRow>
              <BackButtonInline
                onClick={() => history.goBack()}
                icon={faArrowLeft}
                text={childName ?? i18n.common.back}
                data-qa="go-back"
              />
              {childResult && (
                <IconButton
                  size="L"
                  icon={faUserUnlock}
                  data-qa="button-logout"
                  onClick={() => setLoggingOut(true)}
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
                      <PinInput
                        onPinChange={setSelectedPin}
                        pin={selectedPin}
                        info={getInputInfo()}
                        inputRef={pinInputRef}
                      />
                      {selectedPin.join('').length === 4 && (
                        <IconButton
                          icon={faArrowRight}
                          onClick={loadChildSensitiveInfo}
                          data-qa="submit-pin"
                        />
                      )}
                    </FixedSpaceRow>
                  </>
                )}
              </FixedSpaceColumn>
            </ContentArea>
          )}
        </TallContentAreaNoOverflow>
      </>
    )
  })
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
