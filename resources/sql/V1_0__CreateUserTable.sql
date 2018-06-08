CREATE TABLE IF NOT EXISTS apriori.user (
    id UUID PRIMARY KEY NOT NULL,
    email VARCHAR(225) NOT NULL,
    salt VARCHAR(225) NOT NULL,
    password VARCHAR(225) NOT NULL
);

CREATE TABLE IF NOT EXISTS apriori.client (
    id UUID PRIMARY KEY NOT NULL, --32-char string
    secret VARCHAR(225) NOT NULL, --php|bin2hex(openssl_random_pseudo_bytes(32));
    name VARCHAR(225) NOT NULL,
    redirect_url TEXT,
    user_id UUID
);

CREATE TABLE IF NOT EXISTS apriori.authorization_code (
    authorization_code VARCHAR(225) NOT NULL,
    client_id UUID NOT NULL,
    user_id UUID references apriori.user(id) NOT NULL,
    redirect_uri VARCHAR(225) NOT NULL,
    expires TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS apriori.access_token (
    access_token UUID,
    client_id UUID references apriori.client(id) NOT NULL,
    user_id UUID references apriori.user(id) NOT NULL,
    scope VARCHAR(225) NOT NULL,
    expires TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS apriori.project (
    id UUID PRIMARY KEY NOT NULL,
    name VARCHAR(225) NOT NULL,
    author UUID references apriori.user(id) NOT NULL,
    transactions BIGINT NOT NULL,
    modified TIMESTAMP,
    created TIMESTAMP
);

CREATE TABLE IF NOT EXISTS apriori.frequencies (
    project_id UUID references apriori.project(id) NOT NULL,
    transaction JSONB NOT NULL,
    frequency BIGINT,
    PRIMARY KEY(project_id, transaction)
);

CREATE TABLE IF NOT EXISTS apriori.associations (
    project_id UUID references apriori.project(id) NOT NULL,
    association JSONB NOT NULL,
    support DECIMAL,
    confidence DECIMAL,
    lift DECIMAL,
    created TIMESTAMP,
    PRIMARY KEY(project_id, association, created)
);


--> create user
--> user creates client
--> user signs into client
--> client (auth_code) requests access_token
--> client (access_token) /creates-project
--> client (access_token) /lists-users
--> client -> transactions

-- ===Management===
-- GET      /management/users
-- PUT      /management/project/create
-- GET      /management/project/{id}
-- GET      /management/project/{id}/associations
-- POST     /management/project/{id}/transactions
-- POST     /management/project/{id}/edit
-- DELETE   /management/project/remove

-- ===Auth===
-- POST     /oauth/authorize
-- GET      /oauth/request_token
-- GET      /oauth/access_token
-- GET      /user/info
-- PUT      /user/app