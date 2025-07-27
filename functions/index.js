// The Cloud Functions for Firebase SDK to create Cloud Functions and set up triggers.
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const Razorpay = require("razorpay");

admin.initializeApp();

// ✅ RAZORPAY FUNCTION — remains unchanged
exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const amount = data.amount;
  const currency = data.currency;

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
    const order = await instance.orders.create(options);
    console.log("Razorpay Order Created:", order);
    return { orderId: order.id, amount: order.amount };
  } catch (error) {
    console.error("Razorpay order creation failed:", error);
    throw new functions.https.HttpsError(
      "internal",
      "Failed to create Razorpay order."
    );
  }
});

exports.notifyOnNewEvent = functions.firestore
  .document("events/{eventId}")
  .onCreate(async (snap, context) => {
    const newEvent = snap.data();
    const eventId = context.params.eventId;

    console.log("🚀 New event created!");

    try {
      const tokensSnapshot = await admin.firestore().collection("tokens").get();
      const tokens = [];

      tokensSnapshot.forEach(doc => {
        if (doc.id !== newEvent.userId) {
          const token = doc.data().token;
          if (token) tokens.push(token);
        }
      });

      if (tokens.length === 0) {
        console.log("❌ No tokens to notify.");
        return null;
      }

      const message = {
        notification: {
          title: `New Event: ${newEvent.title}`,
          body: newEvent.locationAddress || "Tap to view details",
        },
        data: {
          eventId: eventId
        },
      };

      // ✅ Changed from sendMulticast to sendEachForMulticast
      const response = await admin.messaging().sendEachForMulticast({
        ...message,
        tokens,
      });

      console.log(`✅ Notifications sent: ${response.successCount}`);
      return response;
    } catch (error) {
      console.error("🔥 Error sending FCM notifications:", error);
      return null;
    }
  });