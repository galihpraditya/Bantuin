-- ============================================================================
-- 007 - Pastikan hak akses tabel chat dapat digunakan PostgREST
-- ============================================================================
-- RLS policy di 004 sudah benar, tetapi tidak ada GRANT eksplisit untuk
-- booking_messages. Tanpa grant ini, query/insert ditolak dengan 42501
-- ("permission denied for table booking_messages") meskipun policy lolos.
begin;

revoke all on public.booking_messages from anon;
grant select, insert on public.booking_messages to authenticated;

-- Pastikan perubahan Realtime tetap aktif (004 sudah melakukan ini; blok ini
-- membuat migrasi aman dijalankan ulang).
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'booking_messages'
    ) then
        alter publication supabase_realtime add table public.booking_messages;
    end if;
end $$;

commit;
