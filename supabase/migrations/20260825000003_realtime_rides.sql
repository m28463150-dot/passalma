alter table public.rides replica identity full;
alter table public.driver_locations replica identity full;

DO $$
begin
  alter publication supabase_realtime add table public.rides;
exception
  when duplicate_object then null;
end $$;

DO $$
begin
  alter publication supabase_realtime add table public.driver_locations;
exception
  when duplicate_object then null;
end $$;
