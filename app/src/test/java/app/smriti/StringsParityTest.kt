package app.smriti

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Tamil and English must offer exactly the same strings with the same placeholders. */
class StringsParityTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }

    private fun load(dir: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val n = nodes.item(i)
            n.attributes.getNamedItem("name").nodeValue to n.textContent
        }
    }

    private val placeholder = Regex("%\\d+\\$[sd]")

    @Test
    fun `tamil and english have the same keys and placeholders`() {
        val en = load("values")
        val ta = load("values-ta")
        assertEquals("keys missing in Tamil", emptySet<String>(), en.keys - ta.keys)
        assertEquals("keys missing in English", emptySet<String>(), ta.keys - en.keys)
        en.forEach { (k, v) ->
            assertEquals("placeholders differ for $k", placeholder.findAll(v).map { it.value }.toSet(), placeholder.findAll(ta.getValue(k)).map { it.value }.toSet())
        }
    }

    @Test
    fun `tamil strings are actually written in tamil script`() {
        val ta = load("values-ta")
        val latinOnly = setOf("lang_english")
        val tamilBlock = Regex("[\\u0B80-\\u0BFF]")
        ta.filterKeys { it !in latinOnly }.forEach { (k, v) -> assertTrue("$k is not Tamil: $v", tamilBlock.containsMatchIn(v)) }
    }
}
