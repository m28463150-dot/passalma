insert into storage.buckets (id, name, public)
values ('profile-images', 'profile-images', true)
on conflict (id) do update set public = true;

drop policy if exists profile_images_read on storage.objects;
create policy profile_images_read on storage.objects
for select using (bucket_id = 'profile-images');

drop policy if exists profile_images_write_own on storage.objects;
create policy profile_images_write_own on storage.objects
for insert to authenticated
with check (bucket_id = 'profile-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists profile_images_update_own on storage.objects;
create policy profile_images_update_own on storage.objects
for update to authenticated
using (bucket_id = 'profile-images' and (storage.foldername(name))[1] = auth.uid()::text)
with check (bucket_id = 'profile-images' and (storage.foldername(name))[1] = auth.uid()::text);
