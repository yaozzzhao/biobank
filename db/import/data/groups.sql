-- Add a default super admin group.
insert into principal (discriminator, id, version, name, activity_status_id)
select 'BbGroup', coalesce(MAX(id), 0)+1, 0, 'Super Administrators', 1 from principal;

-- add a membership to this super admin role
insert into membership(id, version, principal_id, not_null_center_id, not_null_study_id, user_manager, every_permission)
select (select coalesce(MAX(id), 0)+1 from membership),
0, id, 0, 0, b'1', b'1' from principal where name='Super Administrators';

