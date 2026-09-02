CREATE TABLE admin_role (
    id BIGINT NOT NULL,
    code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_admin_role PRIMARY KEY (id),
    CONSTRAINT uk_admin_role_code UNIQUE (code),
    CONSTRAINT ck_admin_role_code CHECK (
        code IN ('CONTENT_ADMIN', 'RISK_REVIEWER', 'SECURITY_REVIEWER', 'SUPER_ADMIN')
    )
);

CREATE TABLE account_admin_role (
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_account_admin_role PRIMARY KEY (id),
    CONSTRAINT uk_account_admin_role UNIQUE (account_id, role_id),
    CONSTRAINT fk_account_admin_role_account FOREIGN KEY (account_id) REFERENCES user_account (id),
    CONSTRAINT fk_account_admin_role_role FOREIGN KEY (role_id) REFERENCES admin_role (id),
    CONSTRAINT fk_account_admin_role_granter FOREIGN KEY (granted_by) REFERENCES user_account (id)
);

CREATE INDEX idx_account_admin_role_account ON account_admin_role (account_id);
CREATE INDEX idx_account_admin_role_role ON account_admin_role (role_id);

CREATE TABLE admin_role_audit_log (
    id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    target_account_id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    action VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_admin_role_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_admin_role_audit_operator FOREIGN KEY (operator_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_admin_role_audit_target FOREIGN KEY (target_account_id) REFERENCES user_account (id),
    CONSTRAINT ck_admin_role_audit_action CHECK (action IN ('GRANT', 'REVOKE'))
);

CREATE INDEX idx_admin_role_audit_target ON admin_role_audit_log (target_account_id, occurred_at);
CREATE INDEX idx_admin_role_audit_operator ON admin_role_audit_log (operator_account_id, occurred_at);

INSERT INTO admin_role (id, code, display_name, description, created_at) VALUES
    (1, 'CONTENT_ADMIN', '内容管理员', '负责帖子、评论和圈子等内容审核', CURRENT_TIMESTAMP(3)),
    (2, 'RISK_REVIEWER', '风险审核员', '负责风险用户、限制和处罚审核', CURRENT_TIMESTAMP(3)),
    (3, 'SECURITY_REVIEWER', '安全审核员', '负责严重举报和安全事件审核', CURRENT_TIMESTAMP(3)),
    (4, 'SUPER_ADMIN', '超级管理员', '负责后台角色管理及最高权限操作', CURRENT_TIMESTAMP(3));
