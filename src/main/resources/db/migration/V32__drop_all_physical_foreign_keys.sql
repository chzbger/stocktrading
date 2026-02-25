ALTER TABLE broker_infos RENAME TO broker_infos_old;
CREATE TABLE broker_infos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    app_key VARCHAR(255),
    app_secret VARCHAR(255),
    account_number VARCHAR(255)
);
INSERT INTO broker_infos (id, user_id, broker_type, app_key, app_secret, account_number)
SELECT id, user_id, broker_type, app_key, app_secret, account_number
FROM broker_infos_old;
DROP TABLE broker_infos_old;
