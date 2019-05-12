create table users (
    id bigserial primary key,
    user_name varchar not null unique,
    first_name varchar not null,
    last_name varchar not null,
    email varchar not null,
    hash varchar not null
);

create table rooms (
    id bigserial primary key,
    name varchar not null,
    members varchar[] not null
);

create table messages (
    id bigserial primary key,
    userId bigserial references users(id),
    roomId bigserial references rooms(id),
    content varchar not null
);