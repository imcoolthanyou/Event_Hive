// The Cloud Functions for Firebase SDK to create Cloud Functions and set up triggers.
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const Razorpay = require("razorpay");
const cors = require("cors")({ origin: true });

admin.initializeApp();

// This is the main function that your app will call.
exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
  // Check if the user is authenticated.
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  // Get the amount and currency from the app's request.
  const amount = data.amount; // in paise, e.g., 1000 for ₹10.00
  const currency = data.currency; // e.g., "INR"

  // --- IMPORTANT: Set your Razorpay keys securely ---
  // We will set these using the terminal, NOT hardcode them here.
  const razorpayKeyId = functions.config().razorpay.key_id;
  const razorpayKeySecret = functions.config().razorpay.key_secret;

  const instance = new Razorpay({
    key_id: razorpayKeyId,
    key_secret: razorpayKeySecret,
  });

  const options = {
    amount: amount,
    currency: currency,
    receipt: `receipt_order_${Math.random().toString(36).substring(7)}`,
  };

  try {
    // Ask Razorpay to create the order.
    const order = await instance.orders.create(options);
    console.log("Razorpay Order Created:", order);
    // Send the order details back to your app.
    return { orderId: order.id, amount: order.amount };
  } catch (error) {
    console.error("Razorpay order creation failed:", error);
    throw new functions.https.HttpsError(
      "internal",
      "Failed to create Razorpay order."
    );
  }
});
