drop policy if exists rides_read_requested_driver on public.rides;
create policy rides_read_requested_driver on public.rides
for select to authenticated
using (
  status = 'requested'
  and exists (
    select 1 from public.profiles
    where profiles.id = auth.uid()
      and profiles.role = 'driver'
      and profiles.approved = true
  )
);

drop policy if exists rides_accept_driver on public.rides;
create policy rides_accept_driver on public.rides
for update to authenticated
using (
  status = 'requested'
  and driver_id is null
  and exists (
    select 1 from public.profiles
    where profiles.id = auth.uid()
      and profiles.role = 'driver'
      and profiles.approved = true
  )
)
with check (driver_id = auth.uid() and status = 'accepted');
