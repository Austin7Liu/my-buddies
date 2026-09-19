package com.austin.module.search.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class ElasticsearchSearchProviderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void partialFieldsUseNgramAnalyzerAndParticipateInSearch() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/search/index-v1.json")) {
            JsonNode root = objectMapper.readTree(input);

            assertThat(root.at("/settings/analysis/tokenizer/my_buddies_partial_tokenizer/type").asText())
                    .isEqualTo("ngram");
            assertThat(root.at("/settings/index.max_ngram_diff").asInt()).isEqualTo(19);
            assertThat(root.at("/settings/analysis/tokenizer/my_buddies_partial_tokenizer/min_gram").asInt())
                    .isEqualTo(1);
            assertThat(root.at("/mappings/properties/title/fields/partial/analyzer").asText())
                    .isEqualTo("my_buddies_partial");
            assertThat(root.at("/mappings/properties/topicName/fields/partial/search_analyzer").asText())
                    .isEqualTo("my_buddies_chinese");
            assertThat(root.at("/mappings/properties/circleName/fields/partial/analyzer").asText())
                    .isEqualTo("my_buddies_partial");
        }

        assertThat(ElasticsearchSearchProvider.SEARCH_FIELDS)
                .contains("title.partial^1.5", "topicName.partial^1.2", "circleName.partial^1.2");
    }
}
