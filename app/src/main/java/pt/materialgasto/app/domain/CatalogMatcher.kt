package pt.materialgasto.app.domain

import pt.materialgasto.app.data.local.entity.CatalogItemEntity
import java.text.Normalizer

sealed interface DesignacaoLookupResult {
    data object NoExactMatch : DesignacaoLookupResult
    data class ExactUniqueMatch(val item: CatalogItemEntity) : DesignacaoLookupResult
    data class MultipleMatches(val items: List<CatalogItemEntity>) : DesignacaoLookupResult
}

object CatalogMatcher {

    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("\\s+"), " ")
    }

    fun suggestByReferencia(
        query: String,
        catalog: List<CatalogItemEntity>,
        limit: Int = 8
    ): List<CatalogItemEntity> {
        val norm = normalize(query)
        if (norm.isBlank()) return emptyList()

        val startsWith = mutableListOf<CatalogItemEntity>()
        val contains = mutableListOf<CatalogItemEntity>()
        for (item in catalog) {
            when {
                item.referenciaNormalized.startsWith(norm) -> startsWith.add(item)
                item.referenciaNormalized.contains(norm) -> contains.add(item)
            }
        }
        return (startsWith + contains).take(limit)
    }

    fun suggestByDesignacao(
        query: String,
        catalog: List<CatalogItemEntity>,
        limit: Int = 8
    ): List<CatalogItemEntity> {
        val norm = normalize(query)
        if (norm.isBlank()) return emptyList()

        val tokens = norm.split(" ").filter { it.isNotBlank() }
        val startsWith = mutableListOf<CatalogItemEntity>()
        val contains = mutableListOf<CatalogItemEntity>()

        for (item in catalog) {
            val target = item.designacaoNormalized
            when {
                target.startsWith(norm) -> startsWith.add(item)
                tokens.all { token -> target.contains(token) } -> contains.add(item)
            }
        }
        return (startsWith + contains).take(limit)
    }

    fun findExactByReferencia(
        referencia: String,
        catalog: List<CatalogItemEntity>
    ): CatalogItemEntity? {
        val norm = normalize(referencia)
        if (norm.isBlank()) return null
        return catalog.firstOrNull { it.referenciaNormalized == norm }
    }

    fun findExactByDesignacao(
        designacao: String,
        catalog: List<CatalogItemEntity>
    ): DesignacaoLookupResult {
        val norm = normalize(designacao)
        if (norm.isBlank()) return DesignacaoLookupResult.NoExactMatch
        val matches = catalog.filter { it.designacaoNormalized == norm }
        return when {
            matches.isEmpty() -> DesignacaoLookupResult.NoExactMatch
            matches.size == 1 -> DesignacaoLookupResult.ExactUniqueMatch(matches.first())
            else -> DesignacaoLookupResult.MultipleMatches(matches)
        }
    }
}
