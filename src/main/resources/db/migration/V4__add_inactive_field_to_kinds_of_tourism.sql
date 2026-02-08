alter table instructors_grades.kinds_of_tourism
    add column inactive boolean default false;
update instructors_grades.kinds_of_tourism
set inactive = true
where title = 'шк.тур';
