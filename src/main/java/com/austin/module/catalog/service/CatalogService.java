package com.austin.module.catalog.service;

import com.austin.module.catalog.domain.Category;
import com.austin.module.catalog.domain.Topic;
import java.util.List;

public interface CatalogService {
    List<Category> listCategories(boolean includeDisabled);
    List<Topic> listTopics(long categoryId, boolean includeDisabled);
    Topic getTopic(long topicId, boolean includeDisabled);
    Category createCategory(long operatorId, String code, String name, String description, int sortOrder);
    Category updateCategory(long operatorId, long categoryId, String name, String description, int sortOrder);
    Category setCategoryEnabled(long operatorId, long categoryId, boolean enabled);
    Topic createTopic(long operatorId, long categoryId, String code, String name, String description, int sortOrder);
    Topic updateTopic(long operatorId, long topicId, String name, String description, int sortOrder);
    Topic setTopicEnabled(long operatorId, long topicId, boolean enabled);
}

