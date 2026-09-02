package com.austin.module.catalog.service;

import com.austin.common.exception.ConflictException;
import com.austin.common.exception.ResourceNotFoundException;
import com.austin.module.catalog.domain.CatalogAdminAuditLog;
import com.austin.module.catalog.domain.CatalogAuditAction;
import com.austin.module.catalog.domain.CatalogEntityType;
import com.austin.module.catalog.domain.Category;
import com.austin.module.catalog.domain.Topic;
import com.austin.module.catalog.mapper.CatalogAdminAuditLogMapper;
import com.austin.module.catalog.mapper.CategoryMapper;
import com.austin.module.catalog.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {
    private final CategoryMapper categoryMapper;
    private final TopicMapper topicMapper;
    private final CatalogAdminAuditLogMapper auditMapper;
    private final Clock clock;

    @Override @Transactional(readOnly = true)
    public List<Category> listCategories(boolean includeDisabled) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder).orderByAsc(Category::getId);
        if (!includeDisabled) query.eq(Category::getEnabled, true);
        return categoryMapper.selectList(query);
    }

    @Override @Transactional(readOnly = true)
    public List<Topic> listTopics(long categoryId, boolean includeDisabled) {
        Category category = requireCategory(categoryId);
        if (!includeDisabled && !category.getEnabled()) throw new ResourceNotFoundException("分类不存在");
        LambdaQueryWrapper<Topic> query = new LambdaQueryWrapper<Topic>()
                .eq(Topic::getCategoryId, categoryId)
                .orderByAsc(Topic::getSortOrder).orderByAsc(Topic::getId);
        if (!includeDisabled) query.eq(Topic::getEnabled, true);
        return topicMapper.selectList(query);
    }

    @Override @Transactional(readOnly = true)
    public Topic getTopic(long topicId, boolean includeDisabled) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null || (!includeDisabled && !topic.getEnabled()))
            throw new ResourceNotFoundException("话题不存在");
        Category category = requireCategory(topic.getCategoryId());
        if (!includeDisabled && !category.getEnabled()) throw new ResourceNotFoundException("话题不存在");
        return topic;
    }

    @Override @Transactional
    public Category createCategory(long operatorId, String code, String name, String description, int sortOrder) {
        LocalDateTime now = LocalDateTime.now(clock);
        Category value = Category.builder().code(code.trim()).name(name.trim()).description(trim(description))
                .sortOrder(sortOrder).enabled(true).version(0).createdAt(now).updatedAt(now).build();
        try { categoryMapper.insert(value); }
        catch (DuplicateKeyException ex) { throw new ConflictException("分类编码或名称已存在", ex); }
        audit(operatorId, CatalogEntityType.CATEGORY, value.getId(), CatalogAuditAction.CREATE, now);
        return value;
    }

    @Override @Transactional
    public Category updateCategory(long operatorId, long id, String name, String description, int sortOrder) {
        Category value = requireCategory(id); LocalDateTime now = LocalDateTime.now(clock);
        value.setName(name.trim()); value.setDescription(trim(description)); value.setSortOrder(sortOrder); value.setUpdatedAt(now);
        updateCategory(value); audit(operatorId, CatalogEntityType.CATEGORY, id, CatalogAuditAction.UPDATE, now); return value;
    }

    @Override @Transactional
    public Category setCategoryEnabled(long operatorId, long id, boolean enabled) {
        Category value = requireCategory(id);
        if (value.getEnabled() == enabled) return value;
        LocalDateTime now = LocalDateTime.now(clock); value.setEnabled(enabled); value.setUpdatedAt(now); updateCategory(value);
        audit(operatorId, CatalogEntityType.CATEGORY, id, enabled ? CatalogAuditAction.ENABLE : CatalogAuditAction.DISABLE, now);
        return value;
    }

    @Override @Transactional
    public Topic createTopic(long operatorId, long categoryId, String code, String name, String description, int sortOrder) {
        requireCategory(categoryId); LocalDateTime now = LocalDateTime.now(clock);
        Topic value = Topic.builder().categoryId(categoryId).code(code.trim()).name(name.trim())
                .description(trim(description)).sortOrder(sortOrder).enabled(true).version(0).createdAt(now).updatedAt(now).build();
        try { topicMapper.insert(value); }
        catch (DuplicateKeyException ex) { throw new ConflictException("话题编码或分类内名称已存在", ex); }
        audit(operatorId, CatalogEntityType.TOPIC, value.getId(), CatalogAuditAction.CREATE, now); return value;
    }

    @Override @Transactional
    public Topic updateTopic(long operatorId, long id, String name, String description, int sortOrder) {
        Topic value = requireTopic(id); LocalDateTime now = LocalDateTime.now(clock);
        value.setName(name.trim()); value.setDescription(trim(description)); value.setSortOrder(sortOrder); value.setUpdatedAt(now);
        updateTopic(value); audit(operatorId, CatalogEntityType.TOPIC, id, CatalogAuditAction.UPDATE, now); return value;
    }

    @Override @Transactional
    public Topic setTopicEnabled(long operatorId, long id, boolean enabled) {
        Topic value = requireTopic(id);
        if (value.getEnabled() == enabled) return value;
        if (enabled && !requireCategory(value.getCategoryId()).getEnabled()) throw new ConflictException("分类停用时不能启用话题");
        LocalDateTime now = LocalDateTime.now(clock); value.setEnabled(enabled); value.setUpdatedAt(now); updateTopic(value);
        audit(operatorId, CatalogEntityType.TOPIC, id, enabled ? CatalogAuditAction.ENABLE : CatalogAuditAction.DISABLE, now); return value;
    }

    private Category requireCategory(long id) { Category v = categoryMapper.selectById(id); if (v == null) throw new ResourceNotFoundException("分类不存在"); return v; }
    private Topic requireTopic(long id) { Topic v = topicMapper.selectById(id); if (v == null) throw new ResourceNotFoundException("话题不存在"); return v; }
    private void updateCategory(Category v) { try { if (categoryMapper.updateById(v) != 1) throw new ConflictException("分类已发生变化，请刷新后重试"); } catch (DuplicateKeyException ex) { throw new ConflictException("分类名称已存在", ex); } }
    private void updateTopic(Topic v) { try { if (topicMapper.updateById(v) != 1) throw new ConflictException("话题已发生变化，请刷新后重试"); } catch (DuplicateKeyException ex) { throw new ConflictException("分类内话题名称已存在", ex); } }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void audit(long operatorId, CatalogEntityType type, long entityId, CatalogAuditAction action, LocalDateTime now) {
        auditMapper.insert(CatalogAdminAuditLog.builder().operatorAccountId(operatorId).entityType(type)
                .entityId(entityId).action(action).occurredAt(now).build());
    }
}

