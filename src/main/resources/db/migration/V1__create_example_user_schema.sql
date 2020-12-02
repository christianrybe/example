create table example_user (
    created             timestamp with time zone,
    modified            timestamp with time zone,
    user_id             uuid primary key,
    first_name          text,
    last_name           text,
    preferred_timezone  text,
    avatar_url          text
);
