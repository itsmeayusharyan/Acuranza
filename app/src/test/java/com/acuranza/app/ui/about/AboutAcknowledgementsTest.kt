package com.acuranza.app.ui.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The acknowledgements data: real modules, real versions, correct licences, safe links. */
class AboutAcknowledgementsTest {

    @Test fun licencesAreCorrect() {
        val sqlcipher = AboutAcknowledgements.libraries.first { it.name.startsWith("SQLCipher") }
        assertEquals(AboutAcknowledgements.BSD, sqlcipher.licence)
        val junit = AboutAcknowledgements.libraries.first { it.name.startsWith("JUnit") }
        assertEquals(AboutAcknowledgements.EPL, junit.licence)
        AboutAcknowledgements.libraries
            .filter { it !== sqlcipher && it !== junit }
            .forEach { assertEquals(AboutAcknowledgements.APACHE, it.licence) }
    }

    @Test fun everyEntryIsCompleteAndHttps() {
        AboutAcknowledgements.libraries.forEach { lib ->
            assertTrue(lib.name.isNotBlank())
            assertTrue(lib.version.isNotBlank())
            assertTrue(lib.url.startsWith("https://"))
            assertTrue(lib.group.isNotBlank())
        }
    }

    @Test fun groupsAreOrderedAndCoverEveryLibrary() {
        assertEquals(AboutAcknowledgements.libraries.map { it.group }.distinct(), AboutAcknowledgements.groups)
        assertEquals(15, AboutAcknowledgements.libraries.size)
    }
}
