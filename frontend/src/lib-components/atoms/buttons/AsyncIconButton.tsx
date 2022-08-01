// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import IconButton, { IconButtonProps } from './IconButton'

export interface AsyncIconButtonProps extends IconButtonProps {
  onClick: (e: React.MouseEvent<HTMLButtonElement>) => Promise<void>
}

export default React.memo(function AsyncIconButton({
  onClick,
  ...props
}: AsyncIconButtonProps) {
  const [disabled, setDisabled] = useState(false)

  return (
    <IconButton
      onClick={async (e) => {
        setDisabled(true)
        await onClick(e)
        setDisabled(false)
      }}
      disabled={disabled || props.disabled}
      {...props}
    />
  )
})
