CREATE TABLE broker_infos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    broker_type VARCHAR(20) NOT NULL,
    app_key VARCHAR(255),
    app_secret VARCHAR(255),
    account_number VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO broker_infos (user_id, broker_type, app_key, app_secret, account_number)
SELECT id, 'KIS', kis_app_key, kis_app_secret, kis_account_number
FROM users
WHERE kis_app_key IS NOT NULL;

INSERT INTO broker_infos (user_id, broker_type, app_key, app_secret, account_number)
SELECT id, 'LS', ls_app_key, ls_app_secret, ls_account_number
FROM users
WHERE ls_app_key IS NOT NULL;

ALTER TABLE users ADD COLUMN active_broker_id BIGINT;

UPDATE users u
SET active_broker_id = (
    SELECT id FROM broker_infos bi 
    WHERE bi.user_id = u.id AND bi.broker_type = 'KIS'
    LIMIT 1
);

UPDATE users u
SET active_broker_id = (
    SELECT id FROM broker_infos bi 
    WHERE bi.user_id = u.id AND bi.broker_type = 'LS'
    LIMIT 1
)
WHERE active_broker_id IS NULL;

ALTER TABLE users DROP COLUMN broker_type;
ALTER TABLE users DROP COLUMN kis_app_key;
ALTER TABLE users DROP COLUMN kis_app_secret;
ALTER TABLE users DROP COLUMN ls_app_key;
ALTER TABLE users DROP COLUMN ls_app_secret;
ALTER TABLE users DROP COLUMN kis_account_number;
ALTER TABLE users DROP COLUMN ls_account_number;
