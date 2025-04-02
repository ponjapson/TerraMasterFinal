const functions = require("firebase-functions");
const admin = require("firebase-admin");
const stripe = require("stripe")(functions.config().stripe.secret_key); // Get the secret key from Firebase config

// Initialize Firebase Admin if it's not already initialized
if (admin.apps.length === 0) {
  admin.initializeApp();
}

exports.createPaymentIntent = functions.https.onCall(async (data, context) => {
  const { downPaymentAmount, currency = 'php' } = data;

  try {
    if (!downPaymentAmount || isNaN(downPaymentAmount)) {
      throw new Error("Invalid down payment amount");
    }

    // Create the Payment Intent with Stripe
    const paymentIntent = await stripe.paymentIntents.create({
      amount: downPaymentAmount * 100, // Convert to cents
      currency: currency,
      payment_method_types: ['card'],
    });

    return { clientSecret: paymentIntent.client_secret };
  } catch (error) {
    console.error("Error creating payment intent:", error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});
