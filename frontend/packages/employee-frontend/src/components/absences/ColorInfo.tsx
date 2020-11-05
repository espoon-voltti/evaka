// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import './ColorInfo.scss'
import { AbsenceTypes } from '~types/absence'
import ColourInfoItem from '~components/common/ColourInfoItem'

const ColorInfo = () => {
  return (
    <div className={'color-info'}>
      {AbsenceTypes.concat('TEMPORARY_RELOCATION').map((absenceType, index) => (
        <ColourInfoItem type={absenceType} key={index} />
      ))}
    </div>
  )
}

export default ColorInfo
