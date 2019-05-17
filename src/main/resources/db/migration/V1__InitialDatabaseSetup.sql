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
    members bigint[] not null
);

create table messages (
    id bigserial primary key,
    user_id bigint references users(id),
    room_id bigint references rooms(id),
    content varchar not null
);