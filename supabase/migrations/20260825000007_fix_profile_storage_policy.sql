drop policy if exists profile_images_write_own on storage.objects;
create policy profile_images_write_own on storage.objects
for insert to authenticated
with check (
  bucket_id = 'profile-images'
  and split_part(name, '.', 1) = auth.uid()::text
);

drop policy if exists profile_images_update_own on storage.objects;
create policy profile_images_update_own on storage.objects
for update to authenticated
using (bucket_id = 'profile-images' and split_part(name, '.', 1) = auth.uid()::text)
with check (bucket_id = 'profile-images' and split_part(name, '.', 1) = auth.uid()::text);
