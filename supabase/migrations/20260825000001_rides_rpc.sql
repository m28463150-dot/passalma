create or replace function public.find_available_drivers(
  pickup_lat double precision,
  pickup_lng double precision,
  radius_km double precision default 10
)
returns table (driver_id uuid, distance_km double precision)
language sql
security invoker
set search_path = public
as $$
  select dl.driver_id,
         st_distance(
           dl.location,
           st_setsrid(st_makepoint(pickup_lng, pickup_lat), 4326)::geography
         ) / 1000.0 as distance_km
  from public.driver_locations dl
  join public.profiles p on p.id = dl.driver_id
  where dl.is_working = true
    and p.role = 'driver'
    and p.approved = true
    and st_dwithin(
      dl.location,
      st_setsrid(st_makepoint(pickup_lng, pickup_lat), 4326)::geography,
      radius_km * 1000.0
    )
  order by distance_km;
$$;

grant execute on function public.find_available_drivers(double precision, double precision, double precision) to authenticated;

create or replace function public.accept_ride(ride_uuid uuid)
returns public.rides
language plpgsql
security invoker
set search_path = public
as $$
declare
  accepted_ride public.rides;
begin
  update public.rides
     set driver_id = auth.uid(),
         status = 'accepted',
         updated_at = now()
   where id = ride_uuid
     and status = 'requested'
     and driver_id is null
  returning * into accepted_ride;

  if not found then
    raise exception 'ride is no longer available';
  end if;

  return accepted_ride;
end;
$$;

grant execute on function public.accept_ride(uuid) to authenticated;
