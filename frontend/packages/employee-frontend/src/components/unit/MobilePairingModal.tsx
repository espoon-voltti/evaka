// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useState } from 'react'
import styled from 'styled-components'

import { Result, Loading, isSuccess, isLoading, isFailure } from '~api'
import {
  getPairingStatus,
  PairingResponse,
  postPairing,
  postPairingResponse,
  putMobileDeviceName
} from '~api/unit'
import InfoModal from '~components/common/InfoModal'
import { ResponseKey } from '~components/mobile/PairingWizard'
import InputField from '~components/shared/atoms/form/InputField'
import Loader from '~components/shared/atoms/Loader'
import { faPlus } from '~icon-set'
import { useTranslation } from '~state/i18n'
import { UUID } from '~types'

interface Props {
  unitId: UUID
  closeModal: () => void
}

const Bold = styled.span`
  font-weight: 600;
`

const Flex = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`

export default React.memo(function MobilePairingModal({
  unitId,
  closeModal
}: Props) {
  const { i18n } = useTranslation()

  const [phase, setPhase] = useState<1 | 2 | 3>(1)
  const [responseKey, setResponseKey] = useState<string>('')
  const [deviceName, setDeviceName] = useState<string>('')
  const [pairingResponse, setPairingResponse] = useState<
    Result<PairingResponse>
  >(Loading())

  useEffect(() => {
    if (phase === 1) {
      void postPairing(unitId).then(setPairingResponse)
    }
  }, [phase])

  useEffect(() => {
    if (responseKey.length === 10) {
      if (isSuccess(pairingResponse)) {
        void postPairingResponse(
          pairingResponse.data.id,
          pairingResponse.data.challengeKey,
          responseKey
        ).then((res) => {
          if (isSuccess(res)) {
            setPairingResponse(res)
            if (res.data.status === 'READY') {
              setPhase(3)
            }
          }
        })
      }
    }
  }, [responseKey])

  useEffect(() => {
    const polling = setInterval(() => {
      if (isSuccess(pairingResponse)) {
        if (pairingResponse.data.status === 'WAITING_CHALLENGE') {
          void getPairingStatus(pairingResponse.data.id).then((status) => {
            if (isSuccess(status)) {
              if (status.data.status === 'WAITING_RESPONSE') {
                clearInterval(polling)
                setPhase(2)
              }
            }
          })
        }
      }
    }, 1000)
    return () => clearInterval(polling)
  }, [pairingResponse])

  async function saveDeviceName() {
    if (isSuccess(pairingResponse) && pairingResponse.data.mobileDeviceId) {
      await putMobileDeviceName(pairingResponse.data.mobileDeviceId, deviceName)
      closeModal()
    }
  }

  return (
    <Fragment>
      {phase === 1 && (
        <InfoModal
          iconColour={'blue'}
          title={i18n.unit.accessControl.mobileDevices.modalTitle}
          icon={faPlus}
          text={
            <Fragment>
              {i18n.unit.accessControl.mobileDevices.modalText1}
              <br />
              <Bold>{i18n.mobile.mobileUrl}.</Bold>
              <br />
              {i18n.unit.accessControl.mobileDevices.modalText2}
            </Fragment>
          }
          resolve={closeModal}
          resolveLabel={i18n.common.cancel}
        >
          {isLoading(pairingResponse) && <Loader />}
          {isFailure(pairingResponse) && <div>{i18n.common.loadingFailed}</div>}
          {isSuccess(pairingResponse) && (
            <ResponseKey data-qa="challenge-key">
              {pairingResponse.data.challengeKey}
            </ResponseKey>
          )}
        </InfoModal>
      )}

      {phase === 2 && (
        <InfoModal
          iconColour={'blue'}
          title={i18n.unit.accessControl.mobileDevices.modalTitle}
          icon={faPlus}
          text={i18n.unit.accessControl.mobileDevices.modalText3}
          resolve={closeModal}
          resolveLabel={i18n.common.cancel}
        >
          {isLoading(pairingResponse) && <Loader />}
          {isFailure(pairingResponse) && <div>{i18n.common.loadingFailed}</div>}
          {isSuccess(pairingResponse) && (
            <Flex>
              <InputField
                value={responseKey}
                onChange={setResponseKey}
                placeholder={i18n.common.code}
                width={'m'}
                data-qa="response-key-input"
              />
            </Flex>
          )}
        </InfoModal>
      )}

      {phase === 3 && (
        <InfoModal
          iconColour={'green'}
          title={i18n.unit.accessControl.mobileDevices.modalTitle}
          icon={faPlus}
          text={i18n.unit.accessControl.mobileDevices.modalText4}
          reject={closeModal}
          resolve={saveDeviceName}
          resolveLabel={i18n.common.ready}
        >
          {isLoading(pairingResponse) && <Loader />}
          {isFailure(pairingResponse) && <div>{i18n.common.loadingFailed}</div>}
          {isSuccess(pairingResponse) && (
            <Flex>
              {pairingResponse.data.mobileDeviceId ? (
                <InputField
                  value={deviceName}
                  onChange={setDeviceName}
                  placeholder={
                    i18n.unit.accessControl.mobileDevices.editPlaceholder
                  }
                  width={'m'}
                  data-qa="mobile-device-name-input"
                />
              ) : (
                <div>{i18n.common.loadingFailed}</div>
              )}
            </Flex>
          )}
        </InfoModal>
      )}
    </Fragment>
  )
})
