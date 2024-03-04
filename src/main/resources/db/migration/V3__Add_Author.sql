create table author
(
    id         serial primary key,
    full_name  varchar(500),
    created    timestamp without time zone
);

alter table budget
    add author_id int references author (id);



