// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import './ColorInfo.scss'
import { AbsenceTypes } from '~types/absence'
import { useTranslation } from '~state/i18n'
import ColourInfoItem from '~components/common/ColourInfoItem'

const ColorInfo = () => {
  const { i18n } = useTranslation()
  return (
    <div className={'color-info'}>
      {AbsenceTypes.concat('TEMPORARY_RELOCATION').map((absenceType, index) => (
        <ColourInfoItem type={absenceType} key={index} />
      ))}
      <div className={'color-info-container'}>
        <div className={`info-ball info-ball-weekend`} />
        <div>{i18n.absences.weekend}</div>
      </div>
    </div>
  )
}

export default ColorInfo
