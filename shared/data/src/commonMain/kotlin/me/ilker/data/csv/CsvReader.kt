package me.ilker.data.csv

/**
 * Minimal RFC 4180 reader: quoted fields, doubled quotes inside them, and CRLF or LF record ends.
 *
 * A blank line is not a record, so a trailing newline or a spacer row does not turn into an empty
 * transaction. Fields are returned unescaped; interpreting them is [TransactionCsv]'s job.
 */
internal class CsvReader(private val input: String) {

    fun readRecords(): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0
        var hasContent = false

        fun endField() {
            fields += field.toString()
            field.clear()
        }

        fun endRecord() {
            endField()
            if (fields.any { value -> value.isNotBlank() }) {
                records += fields.toList()
            }
            fields = mutableListOf()
            hasContent = false
        }

        while (index < input.length) {
            val char = input[index]
            when {
                inQuotes -> {
                    if (char == '"') {
                        if (index + 1 < input.length && input[index + 1] == '"') {
                            field.append('"')
                            index++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(char)
                    }
                    hasContent = true
                }

                char == '"' && field.isEmpty() -> {
                    inQuotes = true
                    hasContent = true
                }

                char == ',' -> {
                    endField()
                    hasContent = true
                }

                char == '\r' -> {
                    if (index + 1 < input.length && input[index + 1] == '\n') index++
                    endRecord()
                }

                char == '\n' -> endRecord()

                else -> {
                    field.append(char)
                    hasContent = true
                }
            }
            index++
        }

        if (hasContent || field.isNotEmpty() || fields.isNotEmpty()) {
            endRecord()
        }

        return records
    }
}
