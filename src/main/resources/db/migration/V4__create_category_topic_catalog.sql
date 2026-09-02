CREATE TABLE category (
    id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uk_category_code UNIQUE (code),
    CONSTRAINT uk_category_name UNIQUE (name),
    CONSTRAINT ck_category_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE topic (
    id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_topic PRIMARY KEY (id),
    CONSTRAINT uk_topic_code UNIQUE (code),
    CONSTRAINT uk_topic_category_name UNIQUE (category_id, name),
    CONSTRAINT fk_topic_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT ck_topic_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX idx_category_public_order ON category (enabled, sort_order, id);
CREATE INDEX idx_topic_category_public_order ON topic (category_id, enabled, sort_order, id);

CREATE TABLE catalog_admin_audit_log (
    id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    entity_type VARCHAR(16) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_catalog_admin_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_catalog_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_catalog_audit_entity_type CHECK (entity_type IN ('CATEGORY', 'TOPIC')),
    CONSTRAINT ck_catalog_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'ENABLE', 'DISABLE'))
);

CREATE INDEX idx_catalog_audit_entity ON catalog_admin_audit_log (entity_type, entity_id, occurred_at);

INSERT INTO category (id, code, name, description, sort_order, enabled, version, created_at, updated_at) VALUES
    (101, 'sports', '运动', '各类健康运动活动', 10, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (102, 'movies', '电影', '观影及电影交流活动', 20, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (103, 'board-games', '棋牌', '健康合规的棋牌和桌游活动', 30, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (104, 'outdoors', '户外', '徒步、露营等户外活动', 40, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (105, 'learning', '学习', '自习、语言及技能学习活动', 50, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (106, 'music', '音乐', '音乐欣赏与健康音乐活动', 60, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (107, 'exhibitions', '展览', '博物馆、美术馆及展览活动', 70, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (108, 'photography', '摄影', '以摄影实践为主题的活动', 80, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (109, 'gaming', '游戏', '健康合规的电子游戏活动', 90, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    (110, 'other-healthy', '其他健康活动', '其他符合平台定位的健康活动', 100, TRUE, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));

