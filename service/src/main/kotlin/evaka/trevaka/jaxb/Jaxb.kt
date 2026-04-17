// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.jaxb

import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun localDateToXMLGregorianCalendar(localDate: LocalDate): XMLGregorianCalendar =
    DatatypeFactory.newInstance().newXMLGregorianCalendar(localDate.toString())
