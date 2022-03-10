// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { scrollElementToPos } from 'lib-common/utils/scrolling'

export const scrollMainToPos = (opts: ScrollToOptions) =>
  scrollElementToPos(document.getElementById('main'), opts)
