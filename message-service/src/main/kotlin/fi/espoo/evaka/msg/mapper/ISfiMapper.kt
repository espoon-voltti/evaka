// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.mapper

import fi.espoo.evaka.msg.service.sfi.ISfiClientService
import fi.espoo.evaka.msg.sficlient.soap.ArrayOfKohdeWS2A

interface ISfiMapper {
    fun mapToSoapTargets(metadata: ISfiClientService.MessageMetadata): ArrayOfKohdeWS2A
}
