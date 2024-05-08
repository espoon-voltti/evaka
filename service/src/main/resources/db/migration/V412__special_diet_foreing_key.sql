alter table child
    drop constraint fk$diet_id_special_diet_id;

alter table child
    add constraint fk$diet_id_special_diet_id
        foreign key (diet_id) references special_diet
            deferrable;

