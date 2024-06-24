// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.mapper

import fi.espoo.evaka.VtjXroadClientEnv
import fi.espoo.evaka.VtjXroadServiceEnv
import fi.espoo.evaka.vtjclient.mapper.XroadMapper.Companion.factory
import fi.espoo.evaka.vtjclient.soap.ObjectFactory
import fi.espoo.evaka.vtjclient.soap.XRoadClientIdentifierType
import fi.espoo.evaka.vtjclient.soap.XRoadObjectType
import fi.espoo.evaka.vtjclient.soap.XRoadServiceIdentifierType
import jakarta.xml.bind.JAXBElement

fun VtjXroadClientEnv.toClientHeader(): JAXBElement<XRoadClientIdentifierType>? =
    XRoadClientIdentifierType()
        .also {
            it.objectType = XRoadObjectType.SUBSYSTEM
            it.xRoadInstance = instance
            it.memberClass = memberClass
            it.memberCode = memberCode
            it.subsystemCode = subsystemCode
        }.let { factory.createClient(it) }

fun VtjXroadServiceEnv.toServiceHeader(): JAXBElement<XRoadServiceIdentifierType> =
    XRoadServiceIdentifierType()
        .also {
            it.objectType = XRoadObjectType.SERVICE
            it.xRoadInstance = instance
            it.memberClass = memberClass
            it.memberCode = memberCode
            it.subsystemCode = subsystemCode
            it.serviceCode = serviceCode
            it.serviceVersion = serviceVersion
        }.let { factory.createService(it) }

class XroadMapper {
    companion object {
        val factory = ObjectFactory()
    }
}
