// MainActivity.java
package com.quickpay.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "QuickPay";
    private DatabaseHelper dbHelper;
    private WebView webView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize components
        dbHelper = new DatabaseHelper(this);
        setupAWSCredentials();
        initializePaymentSystem();
        setupWebView();
        
        // Debug logging with sensitive data (VUL-007)
        Log.d(TAG, "User session initialized with token: " + getUserToken());
    }
    
    private void setupAWSCredentials() {
        // VUL-002: AWS Credentials in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("aws_config", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("aws_access_key", "AKIA3QUICKPAYTEST2024");
        editor.putString("aws_secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYQUICKPAYSECRET");
        editor.putString("s3_bucket", "quickpay-user-documents");
        editor.apply();
    }
    
    private void initializePaymentSystem() {
        PaymentManager paymentManager = new PaymentManager(this);
        paymentManager.initialize();
    }
    
    private void setupWebView() {
        // VUL-008: Insecure WebView Implementation
        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebViewClient(new WebViewClient());
    }
    
    private String getUserToken() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getString("auth_token", "token_abc123xyz789");
    }
}

// PaymentManager.java
package com.quickpay.app.payments;

import android.content.Context;
import android.util.Log;
import com.stripe.android.Stripe;

public class PaymentManager {
    private static final String TAG = "PaymentManager";
    private Context context;
    
    // VUL-001: Hardcoded Stripe API Secret Key
    private static final String STRIPE_KEY = "sk_live_51H3kP4KL9nQ8vX2M5PqRsTu6YZ7AbC8dEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKl";
    private static final String STRIPE_PUBLISHABLE = "pk_live_51H3kP4KL9nQ8vX2M5PqRsTu6YZ7AbC8d";
    
    // Additional hardcoded secrets for testing
    private static final String PAYPAL_CLIENT_ID = "AYSq3RDGsmBLJE-otTkBtM-jBRd1TCQwFf9RGfwddNXWz0uFU9ztymylOhRS";
    private static final String PAYPAL_SECRET = "EGnHDxD_qRPdaLdZz8iCr8N7_MzF-YHPTkjs6NKYQvQSBngp4PTTVWkPZRbL";
    
    public PaymentManager(Context context) {
        this.context = context;
    }
    
    public void initialize() {
        // Initialize Stripe with production key
        Stripe stripe = new Stripe(context, STRIPE_KEY);
        Log.d(TAG, "Payment system initialized");
    }
    
    public boolean processPayment(String cardNumber, String cvv, double amount) {
        // Process payment logic
        Log.d(TAG, "Processing payment for card: " + cardNumber);
        return true;
    }
}

// DatabaseHelper.java
package com.quickpay.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

public class DatabaseHelper extends SQLiteOpenHelper {
    // VUL-003: Unencrypted SQLite Database with PII
    private static final String DATABASE_NAME = "quickpay.db";
    private static final int DATABASE_VERSION = 1;
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables with sensitive data unencrypted
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT NOT NULL," +
                "full_name TEXT NOT NULL," +
                "ssn TEXT," +
                "phone TEXT," +
                "date_of_birth TEXT" +
                ")");
        
        db.execSQL("CREATE TABLE cards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "card_number TEXT," +
                "card_holder TEXT," +
                "cvv TEXT," +
                "expiry_date TEXT," +
                "billing_address TEXT" +
                ")");
        
        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender_id INTEGER," +
                "receiver_id INTEGER," +
                "amount REAL," +
                "timestamp TEXT," +
                "status TEXT," +
                "card_used TEXT" +
                ")");
        
        // Insert sample data
        insertSampleData(db);
    }
    
    private void insertSampleData(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("email", "john.doe@email.com");
        values.put("full_name", "John Doe");
        values.put("ssn", "123-45-6789");
        values.put("phone", "+1-555-0123");
        values.put("date_of_birth", "1985-03-15");
        db.insert("users", null, values);
        
        values.clear();
        values.put("user_id", 1);
        values.put("card_number", "4532015112830366");
        values.put("card_holder", "JOHN DOE");
        values.put("cvv", "123");
        values.put("expiry_date", "12/25");
        values.put("billing_address", "123 Main St, Anytown, CA 90210");
        db.insert("cards", null, values);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS cards");
        db.execSQL("DROP TABLE IF EXISTS transactions");
        onCreate(db);
    }
}

// PinManager.java
package com.quickpay.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

public class PinManager {
    private static final String TAG = "PinManager";
    private Context context;
    private SharedPreferences prefs;
    
    public PinManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE);
    }
    
    // VUL-005: Weak PIN Encryption (Base64 only)
    public void savePin(String pin) {
        String encodedPin = Base64.encodeToString(pin.getBytes(), Base64.DEFAULT);
        prefs.edit().putString("user_pin", encodedPin).apply();
        Log.d(TAG, "PIN saved: " + encodedPin);
    }
    
    public boolean verifyPin(String inputPin) {
        String savedPin = prefs.getString("user_pin", "");
        String decodedPin = new String(Base64.decode(savedPin, Base64.DEFAULT));
        return inputPin.equals(decodedPin);
    }
}

// TransactionReceiver.java - VUL-004: Exported BroadcastReceiver
package com.quickpay.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class TransactionReceiver extends BroadcastReceiver {
    private static final String TAG = "TransactionReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("com.quickpay.PROCESS_PAYMENT".equals(action)) {
            String userId = intent.getStringExtra("user_id");
            String amount = intent.getStringExtra("amount");
            String recipient = intent.getStringExtra("recipient");
            
            // Process transaction without authentication
            processTransaction(context, userId, amount, recipient);
        }
    }
    
    private void processTransaction(Context context, String userId, String amount, String recipient) {
        Log.d(TAG, "Processing payment: " + userId + " -> " + recipient + " : $" + amount);
        // Transaction logic here
        Toast.makeText(context, "Payment processed: $" + amount, Toast.LENGTH_SHORT).show();
    }
}

// NetworkManager.java - VUL-006: Certificate Pinning Bypass
package com.quickpay.app.network;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class NetworkManager {
    
    public static void setupNetworking() {
        // Disable certificate validation (for "testing")
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // Trust all clients
                }
                
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // Trust all servers - no pinning
                }
            }
        };
        
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// WebAppInterface.java
package com.quickpay.app;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    Context context;
    
    WebAppInterface(Context c) {
        context = c;
    }
    
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }
    
    @JavascriptInterface
    public String getAuthToken() {
        // Exposes auth token to JavaScript
        return context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("auth_token", "");
    }
}