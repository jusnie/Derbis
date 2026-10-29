package com.yasuda.derbis.desktop.core.catalog

import com.yasuda.derbis.desktop.core.model.SearchFilter
import java.util.concurrent.ConcurrentHashMap
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.filter.FilterItem

object SearchFilterCatalog {
    private val cache = ConcurrentHashMap<String, FilterItem>()

    fun contentFilters(service: StreamingService, filter: SearchFilter): List<FilterItem> {
        val key = "${service.serviceId}:${filter.contentFilter}"
        val cached = cache[key]
        if (cached != null) return listOf(cached)
        val resolved = service.searchQHFactory
            .availableContentFilter
            .filterGroups
            .asSequence()
            .flatMap { group -> group.filterItems.asSequence() }
            .firstOrNull { item -> item.name == filter.contentFilter }
            ?: return emptyList()
        cache[key] = resolved
        return listOf(resolved)
    }
}
