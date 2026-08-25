create extension if not exists postgis;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  role text not null check (role in ('customer', 'driver')),
  name text not null default '',
  email text not null default '',
  phone text not null default '',
  profile_image_url text not null default 'default',
  approved boolean not null default false,
  rating numeric not null default 5,
  payout_amount numeric not null default 0,
  car text not null default '',
  license text not null default '',
  service text not null default 'type_1',
  connect_set boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.rides (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.profiles(id),
  driver_id uuid references public.profiles(id),
  service text not null,
  pickup_name text not null default '',
  pickup_lat double precision not null,
  pickup_lng double precision not null,
  destination_name text not null default '',
  destination_lat double precision not null,
  destination_lng double precision not null,
  status text not null default 'requested' check (status in ('requested', 'accepted', 'picked_up', 'completed', 'cancelled')),
  distance_km numeric not null default 0,
  price numeric,
  rating numeric,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.driver_locations (
  driver_id uuid primary key references public.profiles(id) on delete cascade,
  location geography(point, 4326) not null,
  is_working boolean not null default false,
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.rides enable row level security;
alter table public.driver_locations enable row level security;

drop policy if exists profiles_read_own on public.profiles;
create policy profiles_read_own on public.profiles for select using (id = auth.uid());

drop policy if exists profiles_create_own on public.profiles;
create policy profiles_create_own on public.profiles for insert with check (id = auth.uid());

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles for update using (id = auth.uid()) with check (id = auth.uid());

drop policy if exists rides_read_participant on public.rides;
create policy rides_read_participant on public.rides for select using (customer_id = auth.uid() or driver_id = auth.uid());

drop policy if exists rides_create_customer on public.rides;
create policy rides_create_customer on public.rides for insert with check (customer_id = auth.uid());

drop policy if exists rides_update_participant on public.rides;
create policy rides_update_participant on public.rides for update using (customer_id = auth.uid() or driver_id = auth.uid()) with check (customer_id = auth.uid() or driver_id = auth.uid());

drop policy if exists rides_read_requested_driver on public.rides;
create policy rides_read_requested_driver on public.rides for select to authenticated using (
  status = 'requested' and exists (
    select 1 from public.profiles where profiles.id = auth.uid()
      and profiles.role = 'driver' and profiles.approved = true
  )
);

drop policy if exists rides_accept_driver on public.rides;
create policy rides_accept_driver on public.rides for update to authenticated
using (status = 'requested' and driver_id is null and exists (
  select 1 from public.profiles where profiles.id = auth.uid()
    and profiles.role = 'driver' and profiles.approved = true
))
with check (driver_id = auth.uid() and status = 'accepted');

drop policy if exists locations_read_authenticated on public.driver_locations;
create policy locations_read_authenticated on public.driver_locations for select to authenticated using (true);

drop policy if exists locations_write_own on public.driver_locations;
create policy locations_write_own on public.driver_locations for all using (driver_id = auth.uid()) with check (driver_id = auth.uid());

create index if not exists rides_customer_idx on public.rides(customer_id, created_at desc);
create index if not exists rides_driver_idx on public.rides(driver_id, created_at desc);
create index if not exists driver_locations_geo_idx on public.driver_locations using gist(location);

create or replace function public.find_available_drivers(
  pickup_lat double precision,
  pickup_lng double precision,
  radius_km double precision default 10
)
returns table (driver_id uuid, distance_km double precision)
language sql security invoker set search_path = public
as $$
  select dl.driver_id, st_distance(dl.location, st_setsrid(st_makepoint(pickup_lng, pickup_lat), 4326)::geography) / 1000.0
  from public.driver_locations dl join public.profiles p on p.id = dl.driver_id
  where dl.is_working and p.role = 'driver' and p.approved
    and st_dwithin(dl.location, st_setsrid(st_makepoint(pickup_lng, pickup_lat), 4326)::geography, radius_km * 1000.0)
  order by 2;
$$;

grant execute on function public.find_available_drivers(double precision, double precision, double precision) to authenticated;

create or replace function public.accept_ride(ride_uuid uuid)
returns public.rides language plpgsql security invoker set search_path = public
as $$
declare accepted_ride public.rides;
begin
  update public.rides set driver_id = auth.uid(), status = 'accepted', updated_at = now()
  where id = ride_uuid and status = 'requested' and driver_id is null returning * into accepted_ride;
  if not found then raise exception 'ride is no longer available'; end if;
  return accepted_ride;
end;
$$;

grant execute on function public.accept_ride(uuid) to authenticated;
