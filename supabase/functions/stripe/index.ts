import Stripe from "npm:stripe@14.25.0";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY")!, { apiVersion: "2024-04-10" });
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

async function db(path: string, init: RequestInit = {}) {
  return fetch(`${supabaseUrl}/rest/v1/${path}`, {
    ...init,
    headers: {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });
}

async function userId(request: Request): Promise<string> {
  const token = request.headers.get("Authorization")?.replace("Bearer ", "");
  if (!token) throw new Error("Unauthorized");
  const response = await fetch(`${supabaseUrl}/auth/v1/user`, {
    headers: { apikey: serviceKey, Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error("Unauthorized");
  return (await response.json()).id;
}

async function customer(request: Request, uid: string) {
  const existing = await (await db(`payment_customers?user_id=eq.${uid}&select=*`)).json();
  if (existing[0]) return existing[0];
  const profile = await (await db(`profiles?id=eq.${uid}&select=email`)).json();
  const created = await stripe.customers.create({ email: profile[0]?.email || undefined, metadata: { user_id: uid } });
  const saved = await db("payment_customers", {
    method: "POST",
    headers: { Prefer: "return=representation" },
    body: JSON.stringify({ user_id: uid, stripe_customer_id: created.id }),
  });
  return (await saved.json())[0];
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const uid = await userId(request);
    const action = new URL(request.url).searchParams.get("action") ?? "setup";
    const record = await customer(request, uid);
    const stripeCustomer = record.stripe_customer_id;
    let result: unknown;
    if (action === "setup") {
      const setup = await stripe.setupIntents.create({ customer: stripeCustomer, payment_method_types: ["card"] });
      result = { publishableKey: Deno.env.get("STRIPE_PUBLISHABLE_KEY"), clientSecret: setup.client_secret };
    } else if (action === "list") {
      const cards = await stripe.paymentMethods.list({ customer: stripeCustomer, type: "card" });
      const customerData = await stripe.customers.retrieve(stripeCustomer) as Stripe.Customer;
      result = { default_payment_method: customerData.invoice_settings.default_payment_method, cards: cards.data };
    } else if (action === "default") {
      const { payment_id } = await request.json();
      await stripe.paymentMethods.attach(payment_id, { customer: stripeCustomer });
      await stripe.customers.update(stripeCustomer, { invoice_settings: { default_payment_method: payment_id } });
      result = { ok: true };
    } else if (action === "remove") {
      const { payment_id } = await request.json();
      await stripe.paymentMethods.detach(payment_id);
      result = { ok: true };
    } else if (action === "payout") {
      const profile = await (await db(`profiles?id=eq.${uid}&select=payout_amount`)).json();
      const amount = Math.ceil(Number(profile[0]?.payout_amount ?? 0) * 100);
      if (amount <= 0) throw new Error("No payout available");
      const payout = await stripe.payouts.create({ amount, currency: "usd" });
      await db(`profiles?id=eq.${uid}`, { method: "PATCH", body: JSON.stringify({ payout_amount: 0 }) });
      await db("payouts", { method: "POST", body: JSON.stringify({ driver_id: uid, stripe_payout_id: payout.id, amount, status: payout.status }) });
      result = payout;
    } else throw new Error("Unknown action");
    return new Response(JSON.stringify(result), { headers: { ...cors, "Content-Type": "application/json" } });
  } catch (error) {
    return new Response(JSON.stringify({ error: error instanceof Error ? error.message : "Payment error" }), { status: 400, headers: { ...cors, "Content-Type": "application/json" } });
  }
});
