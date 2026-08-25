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

drop policy if exists locations_read_authenticated on public.driver_locations;
create policy locations_read_authenticated on public.driver_locations for select to authenticated using (true);
drop policy if exists locations_write_own on public.driver_locations;
create policy locations_write_own on public.driver_locations for all using (driver_id = auth.uid()) with check (driver_id = auth.uid());

create index if not exists rides_customer_idx on public.rides(customer_id, created_at desc);
create index if not exists rides_driver_idx on public.rides(driver_id, created_at desc);
create index if not exists driver_locations_geo_idx on public.driver_locations using gist(location);
