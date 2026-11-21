# AdMob Monetization & Banner Ad Setup Plan

A complete, practical guide on how you earn money from banner ads in **Fun Learn**, how payouts work, and the exact step-by-step setup to connect your real Google AdMob account to your bank.

---

## 1. How Earning from Banner Ads Works

Banner ads generate revenue in two complementary ways:

1. **Impressions (eCPM - Effective Cost Per Mille):**
   - You earn money simply when players view the banner while playing the game.
   - `eCPM` is the amount advertisers pay per 1,000 views. For educational and children's apps, eCPM typically ranges between **$0.50 to $3.00+ USD** per 1,000 impressions (varying by user geography such as US/UK/EU/India).
   - AdMob automatically refreshes banner ads every 30–60 seconds, meaning a child playing for 10 minutes can generate 10–15 ad impressions in a single session.

2. **Clicks (CPC - Cost Per Click):**
   - When parents or users tap on an ad of interest, advertisers pay an additional cost-per-click fee (often **$0.05 to $0.50+** per click).

3. **Google Play Families Safe-Harbor:**
   - Because our app code enforces `setTagForChildDirectedTreatment(true)` and `MAX_AD_CONTENT_RATING_G`, Google only serves COPPA-compliant, child-safe, age-appropriate educational and family brands. This protects your account and app from policy suspensions while maintaining family advertiser bidding.

---

## 2. Step-by-Step Google AdMob Setup Guide

Follow these sequential steps to set up your account and get paid:

### Step 2.1: Create Your Google AdMob Account
1. Go to [https://admob.google.com](https://admob.google.com) and click **Sign Up**.
2. Sign in with your primary Google account (the same or connected account you use for Google Play).
3. Select your country, time zone, and billing currency.
4. Accept the Google AdMob Terms & Conditions and click **Create AdMob Account**.

---

### Step 2.2: Add "Fun Learn" to AdMob
1. In your AdMob sidebar, click **Apps** > **Add App**.
2. Select the platform: **Android**.
3. Under *"Is the app listed on a supported app store?"*:
   - If your app is already published on Google Play, select **Yes** and search for `Fun Learn` (or your package name `com.aistudio.funlearn.kpmath`), then click **Add**.
   - If you haven't published this exact build yet, select **No**, enter `Fun Learn` as the app name, and click **Add**. *(You can link the store listing later in AdMob settings once live).*

---

### Step 2.3: Create Your Banner Ad Unit
1. Once the app is added, navigate to **Ad units** > **Add Ad Unit**.
2. Select **Banner**.
3. Set an Ad unit name (e.g., `fun_learn_bottom_banner`).
4. Click **Create Ad Unit**.
5. AdMob will give you two important IDs:
   - **AdMob App ID** (looks like: `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`)
   - **Banner Ad Unit ID** (looks like: `ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ`)
6. Copy both IDs down.

---

### Step 2.4: Set Up Payments and Bank Account
1. In the AdMob left menu, click **Payments** > **Payments Info**.
2. Enter your real legal name and tax information (PAN/GST/W-8BEN depending on your country).
3. Add your mailing address:
   - When your earnings reach **$10 USD** (or local currency equivalent), Google will mail an Address Verification PIN via postal mail to confirm your physical address.
4. Add your **Bank Account**:
   - Provide your Account Number, IFSC / SWIFT Code, and Account Holder Name for Direct Electronic Funds Transfer (EFT).

---

## 3. How and When You Receive Payouts

- **Payment Threshold:** You will receive payment once your balance reaches the minimum threshold of **$100 USD** (or equivalent in your currency, e.g., ₹7,000 INR).
- **Payment Schedule:** Google calculates earnings at the end of every calendar month and deposits funds to your bank between the **21st and 26th of the following month**.
- **Tracking:** You can view real-time daily earnings, impressions, eCPM, and click-through rates directly in the AdMob mobile app or web dashboard.

---

## 4. Implementation Steps in the Project

Once you have generated your real AdMob IDs, we will perform the following safe code updates:

### Step 4.1: Update `AndroidManifest.xml`
Replace the test App ID with your real AdMob App ID:
```xml
<!-- Replace test ID with your real AdMob App ID -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-YOUR_PUBLISHER_ID~YOUR_APP_ID" />
```

### Step 4.2: Update `MainActivity.kt`
Replace the test Banner ID with your real Banner Ad Unit ID:
```kotlin
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier.testTag("admob_banner_view"),
    factory = { context ->
      AdView(context).apply {
        setAdSize(AdSize.BANNER)
        // Replace with your real AdMob Banner Ad Unit ID
        adUnitId = "ca-app-pub-YOUR_PUBLISHER_ID/YOUR_BANNER_ID"
        loadAd(AdRequest.Builder().build())
      }
    }
  )
}
```

### Step 4.3: Preserve Google Play Families Verification
Ensure `setTagForChildDirectedTreatment(true)` and `MAX_AD_CONTENT_RATING_G` remain active so Google Play approves your update without review delays or policy warnings.

---

## 5. Verification & Testing

- Before releasing to Google Play, test with test IDs to confirm banner layout and child-friendly responsive rendering.
- Verify that the banner does not overlap any gameplay buttons or navigation bars.
- Once real IDs are inserted and the APK is published, real revenue tracking begins immediately as users play.
