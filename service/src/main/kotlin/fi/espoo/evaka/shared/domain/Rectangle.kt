// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

data class Rectangle(val x: Int, val y: Int, val width: Int, val height: Int) {
    companion object {
        fun fromString(rectangle: String): Rectangle {
            val parts = rectangle.split(",")
            if (parts.size != 4) {
                throw IllegalArgumentException("Rectangle must have 4 parts")
            }
            val intParts =
                try {
                    parts.map { it.toInt() }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Rectangle parts must be integers")
                }
            val (x, y, width, height) = intParts
            return Rectangle(x, y, width, height)
        }

        // Position of recipient's address in the iPost layout design instructions
        val iPostWindowPosition = Rectangle(21, 55, 72, 30)
    }
}
