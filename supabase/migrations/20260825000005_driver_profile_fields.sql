alter table public.profiles add column if not exists car text not null default '';
alter table public.profiles add column if not exists license text not null default '';
alter table public.profiles add column if not exists service text not null default 'type_1';
alter table public.profiles add column if not exists connect_set boolean not null default false;
