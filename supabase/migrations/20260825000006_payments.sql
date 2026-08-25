create table if not exists public.payment_customers (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  stripe_customer_id text not null,
  default_payment_method text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.payment_methods (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  stripe_payment_method_id text not null unique,
  brand text not null default '',
  last4 text not null default '',
  exp_month integer,
  exp_year integer,
  is_default boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.payouts (
  id uuid primary key default gen_random_uuid(),
  driver_id uuid not null references public.profiles(id) on delete cascade,
  stripe_payout_id text unique,
  amount integer not null,
  currency text not null default 'usd',
  status text not null default 'pending',
  created_at timestamptz not null default now()
);

alter table public.payment_customers enable row level security;
alter table public.payment_methods enable row level security;
alter table public.payouts enable row level security;

create policy payment_customers_own on public.payment_customers for select using (user_id = auth.uid());
create policy payment_methods_own on public.payment_methods for select using (user_id = auth.uid());
create policy payouts_own on public.payouts for select using (driver_id = auth.uid());