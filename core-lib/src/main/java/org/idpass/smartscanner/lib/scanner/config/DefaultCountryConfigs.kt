package org.idpass.smartscanner.lib.scanner.config

/**
 * Built-in country configurations for ID document scanning.
 * These are used as defaults when no custom scanIDOCRCountryOptions are provided.
 */
object DefaultCountryConfigs {

    val COLOMBIA = ScanIDOCRCountryOptions(
        countryLabelKey = "COLOMBIA",
        cellLevelOcr = false,
        ocrData = listOf(
            ScanIDOCRField(
                label = "Document ID",
                anchorValue = "NUMERO",
                searchBox = SearchBox(1.1f, -0.5f, 6.0f, 1.7f),
                regex = "[0-9 ]+",
                key = "documentNumber"
            ),
            ScanIDOCRField(
                label = "Last Name",
                anchorValue = "APELLIDOS",
                searchBox = SearchBox(-0.2f, -2.5f, 4.0f, 2.0f),
                regex = "[\\p{L} ]+",
                key = "lastName"
            ),
            ScanIDOCRField(
                label = "First Name",
                anchorValue = "NOMBRES",
                searchBox = SearchBox(-0.2f, -2.5f, 4.0f, 2.0f),
                regex = "[\\p{L} ]+",
                key = "firstName"
            )
        )
    )

    val VENEZUELA = ScanIDOCRCountryOptions(
        countryLabelKey = "VENEZOLANO",
        cellLevelOcr = false,
        dateFormat = "dd/MM/yyyy",
        ocrData = listOf(
            ScanIDOCRField(
                label = "Document ID",
                anchorValue = "Cedula",
                searchBox = SearchBox(-0.25f, 1.2f, 3.0f, 1.5f),
                regex = "[0-9\\.\\- ]+",
                key = "documentNumber"
            ),
            ScanIDOCRField(
                label = "Last Name",
                anchorValue = "APELLIDOS",
                searchBox = SearchBox(1.0f, -0.8f, 4.4f, 1.7f),
                regex = "[\\p{L} ]+",
                key = "lastName"
            ),
            ScanIDOCRField(
                label = "First Name",
                anchorValue = "NOMBRES",
                searchBox = SearchBox(1.0f, -0.8f, 4.8f, 1.9f),
                regex = "[\\p{L} ]+",
                key = "firstName"
            ),
            ScanIDOCRField(
                label = "Birth Date",
                anchorValue = "F. NACIMIENTO",
                searchBox = SearchBox(-0.25f, -1.8f, 1.56f, 1.7f),
                regex = "[0-9]{2}/[0-9]{2}/[0-9]{4}",
                key = "dateOfBirth"
            ),
            ScanIDOCRField(
                label = "Expiry Date",
                anchorValue = "F. VENCIMIENTO",
                searchBox = SearchBox(-0.18f, -1.5f, 1.35f, 1.6f),
                regex = "[0-9]{2}-[0-9]{4}",
                key = "expiryDate"
            )
        )
    )

    val NICARAGUA = ScanIDOCRCountryOptions(
        countryLabelKey = "NICARAGUA",
        dateFormat = "dd-MM-yyyy",
        genderMap = mapOf("M" to "Male", "F" to "Female"),
        ocrData = listOf(
            ScanIDOCRField(
                label = "Document ID",
                anchorValue = "IDENTIDAD",
                searchBox = SearchBox(-0.3f, 1.0f, 2.0f, 1.7f),
                regex = "[0-9\\-A-Za-z]+",
                key = "documentNumber"
            ),
            ScanIDOCRField(
                label = "First Name",
                anchorValue = "Nombres",
                searchBox = SearchBox(-0.2f, 1.0f, 3.5f, 1.5f),
                regex = "[\\p{L} ]+",
                key = "firstName"
            ),
            ScanIDOCRField(
                label = "Last Name",
                anchorValue = "Apellidos",
                searchBox = SearchBox(-0.2f, 1.0f, 2.5f, 2.5f),
                regex = "[\\p{L} ]+",
                key = "lastName"
            ),
            ScanIDOCRField(
                label = "Birth Date",
                anchorValue = "Nacimiento",
                searchBox = SearchBox(-0.1f, 1.1f, 0.9f, 1.5f),
                regex = "[0-9]{2}-[0-9]{2}-[0-9]{4}",
                key = "dateOfBirth"
            ),
            ScanIDOCRField(
                label = "Sex",
                anchorValue = "Sexo",
                searchBox = SearchBox(-0.2f, 1.0f, 1.0f, 1.5f),
                regex = "[MF]",
                key = "gender"
            )
        )
    )

    val GUATEMALA = ScanIDOCRCountryOptions(
        countryLabelKey = "GUATEMALA",
        dateFormat = "ddMMMyyyy",
        genderMap = mapOf("MASCULINO" to "Male", "FEMENINO" to "Female"),
        ocrData = listOf(
            ScanIDOCRField(
                label = "Document ID",
                anchorValue = "Codigo",
                searchBox = SearchBox(-0.2f, 1.0f, 1.4f, 2.5f),
                regex = "[0-9 ]+",
                key = "documentNumber"
            ),
            ScanIDOCRField(
                label = "First Name",
                anchorValue = "Nombre",
                searchBox = SearchBox(-0.2f, 1.0f, 3.0f, 4.0f),
                regex = "[\\p{L} ]+",
                key = "firstName"
            ),
            ScanIDOCRField(
                label = "Last Name",
                anchorValue = "Apellidos",
                searchBox = SearchBox(-0.2f, 0.8f, 2.5f, 4.8f),
                regex = "[\\p{L} ]+",
                key = "lastName"
            ),
            ScanIDOCRField(
                label = "Gender",
                anchorValue = "Sexo",
                searchBox = SearchBox(-0.2f, 0.8f, 4.0f, 1.8f),
                regex = "[\\p{L}]+",
                key = "gender"
            ),
            ScanIDOCRField(
                label = "Birth Date",
                anchorValue = "Nacimiento",
                searchBox = SearchBox(-0.2f, 0.8f, 1.2f, 2.0f),
                regex = "[0-9]{2}[A-Z]{3}[0-9]{4}",
                key = "dateOfBirth"
            )
        )
    )

    val ALL: List<ScanIDOCRCountryOptions> = listOf(COLOMBIA, VENEZUELA, NICARAGUA, GUATEMALA)

    /**
     * Resolves country configurations by merging caller-provided configs with built-in defaults.
     *
     * @param provided Caller-provided country configs (may be null or empty)
     * @param override If true, only use provided configs (ignore defaults entirely).
     *                 If false, merge: provided countries replace matching defaults by countryLabelKey,
     *                 remaining defaults are kept, new countries from provided are appended.
     * @return The resolved list of country configurations
     */
    fun resolve(provided: List<ScanIDOCRCountryOptions>?, override: Boolean = false): List<ScanIDOCRCountryOptions> {
        if (override) return provided ?: emptyList()
        if (provided == null || provided.isEmpty()) return ALL
        val providedKeys = provided.map { it.countryLabelKey }.toSet()
        val retained = ALL.filter { it.countryLabelKey !in providedKeys }
        return provided + retained
    }
}
