// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.archival

import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
internal class Success {
    @get:XmlElement var records: Records? = null

    @get:XmlElement var files: Files? = null

    override fun toString(): String = "Success(records=$records, files=$files)"
}

internal class Records {
    @get:XmlElement var record: Record? = null

    override fun toString(): String = "Records(record=$record)"
}

internal class Record {
    @get:XmlElement var otherId: String? = null

    @get:XmlElement(name = "archiveid") var archiveId: String? = null

    override fun toString(): String = "Record(otherId=$otherId, archiveId=$archiveId)"
}

internal class Files {
    @get:XmlElement var entry: List<FileEntry>? = null

    override fun toString(): String = "Files(entry=$entry)"
}

internal class FileEntry {
    @get:XmlElement var originalId: String? = null

    @get:XmlElement var name: String? = null

    @get:XmlElement(name = "archiveid") var archiveId: String? = null

    override fun toString(): String =
        "FileEntry(originalId=$originalId, name=$name, archiveId=$archiveId)"
}

@XmlRootElement
internal class Errors {
    var error: List<Error>? = null

    override fun toString(): String = "Errors(error=$error)"
}

internal class Error {
    @get:XmlElement(name = "error_code") var errorCode: String? = null

    @get:XmlElement(name = "error_summary") var errorSummary: String? = null

    override fun toString(): String = "Error(errorCode=$errorCode, errorSummary=$errorSummary)"
}
