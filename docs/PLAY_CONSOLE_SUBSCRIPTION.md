# Play Console: yearly subscription + 7-day trial

This app uses product id `premium_yearly` (see `subscription_product_id` in `app/src/main/res/values/strings.xml`). It must match the subscription you create in Play Console.

## Steps

1. **Play Console** → your app → **Monetize** → **Subscriptions** → **Create subscription**.
2. Set **Subscription ID** to `premium_yearly` (must match the app string exactly).
3. Add a **base plan**:
   - **Billing period**: 1 year (P1Y).
   - **Price**: set per region.
4. On that base plan, add **Offers** → **Free trial** → **7 days** (eligibility is managed by Google; typically new subscribers only).
5. **Activate** the subscription and base plan, then roll out a **closed/open testing** track so the product is available to testers.
6. **License testing**: Play Console → **Setup** → **License testing** → add Gmail accounts that can purchase without being charged (for QA).
7. After publishing, users tap **Upgrade** / **Restore purchase** in Settings; the app queries Play Billing and sets **Premium** (ads hidden).

## Restore and reinstall

Entitlement is refreshed from Play on launch (`PlayBillingController.start`) and when opening Settings / tapping **Restore purchase**. Auto Backup excludes `billing_entitlement_prefs.xml`; rely on **Play account purchases**, not local backup, for subscription state.

## Legal

Add subscription terms (auto-renew, trial, cancellation) to your Play listing and optionally in-app (e.g. linked from Settings).
