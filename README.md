# ShopApp — Android E-Commerce App
### Kotlin + Firebase | Full Stack (Frontend + Backend)

---

## 📱 App Flow (from your sketch)

```
Splash (2s logo)
    ↓
Login Screen ──── [User Tab] ──→ Home (Product Grid)
    │                                ↓
    └─── [Admin Tab] ──→ Admin     Product Detail
                         Panel       ↓
                          │        Cart
                          │          ↓
                     Manage        Payment (ABA / AC / COD)
                     Products        ↓
                     Manage        Map + Tracking (30–45 min)
                     Orders          ↓
                                   Success ✅
```

---

## 🗂️ Project Structure

```
app/src/main/java/com/shopapp/
├── data/
│   ├── model/
│   │   └── Models.kt           ← User, Product, CartItem, Order
│   └── repository/
│       └── Repository.kt       ← ALL Firebase logic (auth + firestore)
│
└── ui/
    ├── SplashActivity.kt
    ├── auth/
    │   ├── LoginActivity.kt    ← User/Admin tab switcher
    │   └── RegisterActivity.kt
    ├── home/                   ← USER SIDE
    │   ├── MainActivity.kt     ← Bottom nav host
    │   ├── HomeFragment.kt     ← Product grid + search + categories
    │   ├── ProductAdapter.kt
    │   ├── ProductDetailActivity.kt
    │   ├── CartFragment.kt
    │   ├── CartAdapter.kt
    │   ├── OrdersFragment.kt
    │   ├── OrdersAdapter.kt
    │   └── ProfileFragment.kt
    ├── payment/
    │   ├── PaymentActivity.kt  ← ABA / AC / Delivery
    │   ├── OrderTrackingActivity.kt ← Google Map
    │   └── OrderSuccessActivity.kt
    └── admin/                  ← ADMIN SIDE
        ├── AdminActivity.kt
        ├── AdminProductsFragment.kt
        ├── AdminProductAdapter.kt
        ├── ProductDialogFragment.kt ← Add/Edit dialog
        ├── AdminOrdersFragment.kt
        ├── AdminOrderAdapter.kt
        └── AdminProfileFragment.kt
```

---

## 🔥 Firebase Setup (Step-by-Step)

### Step 1 — Create Firebase Project
1. Go to https://console.firebase.google.com
2. Click **Add project** → name it "ShopApp"
3. Disable Google Analytics (optional) → **Create project**

### Step 2 — Add Android App
1. Click the Android icon (</>) on the project overview
2. Package name: `com.shopapp`
3. Download `google-services.json`
4. Place it in: `app/google-services.json`

### Step 3 — Enable Authentication
1. Firebase Console → **Authentication** → **Get started**
2. Sign-in method → **Email/Password** → Enable → **Save**

### Step 4 — Create Firestore Database
1. Firebase Console → **Firestore Database** → **Create database**
2. Choose **Start in test mode** (for development)
3. Select your region → **Enable**

### Step 5 — Firestore Security Rules (Production)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users: only own profile readable/writable
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      // Cart subcollection
      match /cart/{cartId} {
        allow read, write: if request.auth.uid == userId;
      }
    }

    // Products: anyone can read, only admin can write
    match /products/{productId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }

    // Orders: users see own orders, admin sees all
    match /orders/{orderId} {
      allow create: if request.auth != null;
      allow read: if request.auth != null &&
        (resource.data.userId == request.auth.uid ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin');
      allow update: if request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

### Step 6 — Create Admin / Staff Accounts
Run this ONE TIME to create your admin or staff user. You can do it in Firebase Console:
1. **Authentication** → **Add user** → enter email/password
2. **Firestore** → **users** collection → **Add document**
   - Document ID = the UID from Authentication
   - For **Admin**:
     ```
     uid: "paste-uid-here"
     name: "Admin"
     email: "admin@yourshop.com"
     phone: "012345678"
     role: "admin"
     ```
   - For **Staff**:
     ```
     uid: "paste-uid-here"
     name: "Staff Name"
     email: "staff@yourshop.com"
     phone: "012345678"
     role: "staff"
     ```

---

## 🗺️ Google Maps Setup

### Step 1 — Enable Maps API
1. Go to https://console.cloud.google.com
2. Enable **Maps SDK for Android**
3. Create an API key under **Credentials**

### Step 2 — Add key to AndroidManifest.xml
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_KEY_HERE" />
```

---

## 🏗️ How to Open in Android Studio

1. **File → Open** → select the `ShopApp` folder
2. Wait for Gradle sync to finish
3. Add `google-services.json` to the `app/` folder
4. Replace `YOUR_MAPS_API_KEY_HERE` in `AndroidManifest.xml`
5. Click **Run ▶️**

---

## 🗄️ Firestore Data Structure

```
firestore/
├── users/
│   └── {uid}/
│       ├── name, email, phone, role ("user" | "admin")
│       └── cart/
│           └── {cartItemId}/
│               └── productId, productName, price, quantity
│
├── products/
│   └── {productId}/
│       └── name, description, price, stock, category, imageUrl
│
└── orders/
    └── {orderId}/
        └── userId, userName, items[], totalAmount,
            deliveryAddress, paymentMethod, status,
            latitude, longitude, createdAt
```

---

## 💳 Payment Methods (from your sketch)

| Method | Description |
|--------|-------------|
| **ABA** | ABA Bank — QR / online transfer |
| **AC** | ACLEDA Bank — mobile banking |
| **COD** | Cash on Delivery (your "w. Delivery") |

> To add real payment: integrate ABA PayWay SDK or ACLEDA's API separately.

---

## 🎨 Design Theme

| Color | Hex | Use |
|-------|-----|-----|
| Primary | `#1A1A2E` | Background |
| Primary Dark | `#16213E` | Header / Bottom nav |
| Accent | `#E94560` | Buttons / Highlights |
| Card | `#1E2A4A` | Cards |
| Surface | `#0F3460` | Surfaces |

---

## ✅ Features Summary

### User Side
- [x] Splash screen (2 sec logo)
- [x] Login with User tab
- [x] Register new account
- [x] Browse all products (grid)
- [x] Search products by name
- [x] Filter by category (Food, Drinks, Electronics, Clothing)
- [x] Product detail with Add to Cart
- [x] Cart management (add, remove, quantity)
- [x] Payment screen (ABA / AC / COD)
- [x] Google Maps delivery tracking
- [x] Order success screen
- [x] My Orders history
- [x] Profile & logout

### Admin Side
- [x] Login with Admin tab
- [x] Add new product (name, price, stock, category, image)
- [x] Edit existing product
- [x] Delete product (with confirmation)
- [x] View all orders from all users
- [x] Update order status (pending → confirmed → delivering → delivered)
- [x] Admin profile & logout

---

## 🔧 Troubleshooting

| Issue | Fix |
|-------|-----|
| Gradle sync fails | Check internet; run `./gradlew clean` |
| google-services.json error | Make sure file is inside `app/` folder |
| Map not showing | Check Maps API key + billing enabled |
| Login fails | Confirm Firebase Auth is enabled |
| Products not loading | Check Firestore rules (test mode open) |
