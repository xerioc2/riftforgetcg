create table if not exists public.users (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique,
  avatar text,
  created_at timestamptz not null default now()
);

create table if not exists public.decks (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  champion_card_id text,
  card_list jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.game_rooms (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  status text not null default 'waiting',
  host_id uuid references auth.users(id) on delete set null,
  player_ids uuid[] not null default '{}',
  settings jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.game_rooms
  add column if not exists host_id uuid references auth.users(id) on delete set null;

create table if not exists public.game_states (
  id uuid primary key default gen_random_uuid(),
  room_id uuid not null references public.game_rooms(id) on delete cascade,
  state jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

create unique index if not exists game_states_room_id_key on public.game_states(room_id);

create table if not exists public.match_history (
  id uuid primary key default gen_random_uuid(),
  players jsonb not null,
  winner uuid,
  duration integer,
  deck_snapshots jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now()
);

alter table public.users enable row level security;
alter table public.decks enable row level security;
alter table public.game_rooms enable row level security;
alter table public.game_states enable row level security;
alter table public.match_history enable row level security;

create policy "Users can read their profile"
  on public.users for select
  using (auth.uid() = id);

create policy "Users can update their profile"
  on public.users for all
  using (auth.uid() = id)
  with check (auth.uid() = id);

create policy "Users own decks"
  on public.decks for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "Authenticated users can read rooms"
  on public.game_rooms for select
  to authenticated
  using (true);

create policy "Authenticated users can create rooms"
  on public.game_rooms for insert
  to authenticated
  with check (auth.uid() = any(player_ids));

create policy "Room players can update rooms"
  on public.game_rooms for update
  to authenticated
  using (auth.uid() = any(player_ids))
  with check (auth.uid() = any(player_ids));

create policy "Authenticated users can join waiting rooms"
  on public.game_rooms for update
  to authenticated
  using (status = 'waiting')
  with check (auth.uid() = any(player_ids));

create policy "Room players can delete empty rooms"
  on public.game_rooms for delete
  to authenticated
  using (auth.uid() = any(player_ids));

-- Enable Realtime for game_rooms (run once per project)
ALTER PUBLICATION supabase_realtime ADD TABLE public.game_rooms;

create policy "Authenticated users can read game states"
  on public.game_states for select
  to authenticated
  using (true);

create policy "Authenticated users can read match history"
  on public.match_history for select
  to authenticated
  using (true);

-- Allow authenticated users to insert/update game states for rooms they're in
create policy "Room players can write game state"
  on public.game_states for all
  to authenticated
  using (
    exists (
      select 1 from public.game_rooms
      where id = game_states.room_id
        and auth.uid() = any(player_ids)
    )
  )
  with check (
    exists (
      select 1 from public.game_rooms
      where id = game_states.room_id
        and auth.uid() = any(player_ids)
    )
  );

alter publication supabase_realtime add table public.game_states;

-- Allow server (service role) to write game states
-- The Spring Boot server connects with the Postgres password directly,
-- bypassing RLS. No additional policy needed for server writes.
-- If using anon/service key via REST, add:
create policy "Service role can manage game states"
  on public.game_states for all
  to service_role
  using (true) with check (true);
