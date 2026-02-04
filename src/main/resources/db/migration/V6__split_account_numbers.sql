ALTER TABLE users RENAME COLUMN account_number TO kis_account_number;

ALTER TABLE users ADD COLUMN ls_account_number VARCHAR(255);
