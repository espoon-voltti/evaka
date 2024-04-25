create table special_diet
(
    id           integer PRIMARY KEY not null,
    name         text    not null,
    abbreviation text    not null
);
alter table child
    add diet_id integer;

alter table child
    add constraint fk$diet_id_special_diet_id
        foreign key (diet_id) references special_diet (id)
            on delete NO ACTION on update NO ACTION;

create index idx$child_diet_id
    on public.child (diet_id);

