// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { deleteAttachment } from '../generated/api-clients/attachment'

const q = new Queries()

export const deleteAttachmentMutation = q.mutation(deleteAttachment, [])
