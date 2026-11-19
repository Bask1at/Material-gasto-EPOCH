package pt.materialgasto.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.materialgasto.app.data.local.entity.CatalogItemEntity
import pt.materialgasto.app.domain.CatalogMatcher
import pt.materialgasto.app.domain.DesignacaoLookupResult

class CatalogMatcherTest {

    private fun item(ref: String, desc: String) = CatalogItemEntity(
        referencia = ref,
        designacao = desc,
        referenciaNormalized = CatalogMatcher.normalize(ref),
        designacaoNormalized = CatalogMatcher.normalize(desc)
    )

    private val sampleCatalog = listOf(
        item("ABC123", "Válvula de segurança"),
        item("DEF456", "Tubo de cobre 15mm"),
        item("GHI789", "Filtro de água"),
        item("DUP001", "Junta tórica"),
        item("DUP002", "Junta tórica")
    )

    @Test
    fun exactReferenciaLookupFindsDesignacaoCaseAndAccentInsensitive() {
        val found = CatalogMatcher.findExactByReferencia("abc123", sampleCatalog)
        assertEquals("Válvula de segurança", found?.designacao)
    }

    @Test
    fun exactUniqueDesignacaoLookupFindsReferenciaWithoutAccents() {
        val result = CatalogMatcher.findExactByDesignacao("valvula de seguranca", sampleCatalog)
        assertTrue(result is DesignacaoLookupResult.ExactUniqueMatch)
        assertEquals("ABC123", (result as DesignacaoLookupResult.ExactUniqueMatch).item.referencia)
    }

    @Test
    fun duplicateDesignacaoReturnsMultipleMatches() {
        val result = CatalogMatcher.findExactByDesignacao("junta torica", sampleCatalog)
        assertTrue(result is DesignacaoLookupResult.MultipleMatches)
        assertEquals(2, (result as DesignacaoLookupResult.MultipleMatches).items.size)
    }

    @Test
    fun partialSuggestionsWorkBidirectionally() {
        val refSuggestions = CatalogMatcher.suggestByReferencia("DE", sampleCatalog)
        assertEquals(1, refSuggestions.size)
        assertEquals("DEF456", refSuggestions.first().referencia)

        val descSuggestions = CatalogMatcher.suggestByDesignacao("tubo cobre", sampleCatalog)
        assertEquals(1, descSuggestions.size)
        assertEquals("DEF456", descSuggestions.first().referencia)
    }

    @Test
    fun nonexistentReferenciaReturnsNull() {
        assertNull(CatalogMatcher.findExactByReferencia("NAOEXISTE", sampleCatalog))
    }
}
