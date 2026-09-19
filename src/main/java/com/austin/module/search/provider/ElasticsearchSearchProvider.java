package com.austin.module.search.provider;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.austin.common.exception.SearchTemporarilyUnavailableException;
import com.austin.module.search.config.SearchProperties;
import com.austin.module.search.domain.SearchDocument;
import com.austin.module.search.domain.SearchDocumentType;
import com.austin.module.search.service.ReindexResult;
import com.austin.module.search.service.SearchCriteria;
import com.austin.module.search.service.SearchHitResult;
import com.austin.module.search.service.SearchPage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchSearchProvider implements SearchProvider {

    private static final DateTimeFormatter INDEX_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    static final List<String> SEARCH_FIELDS = List.of(
            "title^3",
            "topicName^2",
            "circleName^2",
            "content",
            "title.partial^1.5",
            "topicName.partial^1.2",
            "circleName.partial^1.2");

    private final ElasticsearchClient client;
    private final SearchProperties properties;
    private final Clock clock;

    @Override
    public SearchPage search(SearchCriteria criteria) {
        try {
            SearchResponse<SearchDocument> response = client.search(request -> request
                    .index(properties.indexAlias())
                    .from((criteria.page() - 1) * criteria.size())
                    .size(criteria.size())
                    .query(query -> query.bool(bool -> {
                        bool.must(must -> must.multiMatch(multi -> multi
                                .query(criteria.keyword())
                                .fields(SEARCH_FIELDS)));
                        bool.filter(filter -> filter.term(term -> term.field("visible").value(true)));
                        if (criteria.type() != null) {
                            bool.filter(filter -> filter.term(term -> term.field("documentType")
                                    .value(criteria.type().name())));
                        }
                        if (criteria.city() != null && !criteria.city().isBlank()) {
                            bool.filter(filter -> filter.term(term -> term.field("city").value(criteria.city())));
                        }
                        if (criteria.meetupMode() != null) {
                            bool.filter(filter -> filter.term(term -> term.field("meetupMode")
                                    .value(criteria.meetupMode().name())));
                        }
                        return bool;
                    }))
                    .highlight(highlight -> highlight.fields(
                            NamedValue.of("title", HighlightField.of(field -> field)),
                            NamedValue.of("content", HighlightField.of(field -> field))))
                    .sort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                    .sort(sort -> sort.field(field -> field.field("createdAt").order(SortOrder.Desc))),
                    SearchDocument.class);
            long total = response.hits().total() == null ? 0 : response.hits().total().value();
            List<SearchHitResult> records = response.hits().hits().stream().map(this::toResult).toList();
            return new SearchPage(records, total, criteria.page(), criteria.size(),
                    (total + criteria.size() - 1) / criteria.size());
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public ReindexResult rebuild(List<SearchDocument> documents) {
        String indexName = properties.indexAlias() + "-v" + INDEX_SUFFIX.format(LocalDateTime.now(clock));
        try (InputStream mapping = new ClassPathResource("search/index-v1.json").getInputStream()) {
            client.indices().create(request -> request.index(indexName).withJson(mapping));
            for (int offset = 0; offset < documents.size(); offset += properties.batchSize()) {
                int start = offset;
                int end = Math.min(start + properties.batchSize(), documents.size());
                BulkResponse response = client.bulk(request -> {
                    request.index(indexName);
                    documents.subList(start, end).forEach(document -> request.operations(operation -> operation
                            .index(index -> index.id(document.id()).document(document))));
                    return request;
                });
                if (response.errors()) {
                    throw new IllegalStateException("部分搜索文档写入失败");
                }
            }
            List<String> oldIndices = oldIndices();
            client.indices().updateAliases(request -> {
                oldIndices.forEach(oldIndex -> request.actions(action -> action.remove(remove -> remove
                        .index(oldIndex).alias(properties.indexAlias()))));
                request.actions(action -> action.add(add -> add.index(indexName).alias(properties.indexAlias())));
                return request;
            });
            return new ReindexResult(indexName, documents.size(), LocalDateTime.now(clock));
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void upsert(SearchDocument document) {
        try {
            requireAlias();
            client.index(request -> request.index(properties.indexAlias()).id(document.id()).document(document));
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void delete(SearchDocumentType type, long businessId) {
        try {
            requireAlias();
            client.delete(request -> request.index(properties.indexAlias()).id(type.name() + ":" + businessId));
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException exception) {
            if (exception.status() != 404) {
                throw unavailable(exception);
            }
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    private void requireAlias() throws IOException {
        if (!client.indices().existsAlias(request -> request.name(properties.indexAlias())).value()) {
            throw new IllegalStateException("搜索索引尚未初始化，请先执行全量重建");
        }
    }

    private List<String> oldIndices() throws IOException {
        try {
            return new ArrayList<>(client.indices().getAlias(request -> request.name(properties.indexAlias()))
                    .aliases().keySet());
        } catch (Exception exception) {
            return List.of();
        }
    }

    private SearchHitResult toResult(Hit<SearchDocument> hit) {
        return new SearchHitResult(hit.source(), hit.score(), firstHighlight(hit, "title"),
                firstHighlight(hit, "content"));
    }

    private String firstHighlight(Hit<SearchDocument> hit, String field) {
        List<String> values = hit.highlight().get(field);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private SearchTemporarilyUnavailableException unavailable(Exception cause) {
        return new SearchTemporarilyUnavailableException("搜索服务暂时不可用，请稍后重试", cause);
    }
}
