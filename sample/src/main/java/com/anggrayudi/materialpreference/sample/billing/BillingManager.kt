/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anggrayudi.materialpreference.sample.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.*
import com.android.billingclient.api.Purchase.PurchasesResult
import com.anggrayudi.materialpreference.sample.BuildConfig
import java.io.IOException
import java.util.*

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
class BillingManager(
        private val activity: Activity,
        private val billingUpdatesListener: BillingUpdatesListener
) : PurchasesUpdatedListener {

    /** A reference to BillingClient  */
    private val billingClient = newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases()
            .build()

    /**
     * True if billing service is connected now.
     */
    private var isServiceConnected: Boolean = false

    private val purchases = ArrayList<Purchase>()

    private var tokensToBeConsumed: MutableSet<String>? = null

    /**
     * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * clien connection response was not received yet.
     */
    var billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED

    /**
     * Listener to the updates that happen when purchase list was updated or consumption of the
     * item was finished
     */
    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onConsumeFinished(token: String, @BillingResponseCode result: Int)
        fun onPurchasesUpdated(purchases: List<Purchase>)
    }

    init {
        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchase items through onPurchasesUpdated() callback.
        startServiceConnection(Runnable {
            // Notifying the listener that billing client is ready
            billingUpdatesListener.onBillingClientSetupFinished()
            // IAB is fully set up. Now, let's get an inventory of stuff we own.
            Log.d(TAG, "Setup successful. Querying inventory.")
            queryPurchases()
        })
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
                billingUpdatesListener.onPurchasesUpdated(this.purchases)
            }
            BillingResponseCode.USER_CANCELED -> Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping")
            else -> Log.w(TAG, "onPurchasesUpdated() got unknown resultCode: ${result.responseCode}")
        }
    }

    /**
     * Start a purchase or subscription replace flow
     */
    fun initiatePurchaseFlow(skuId: String, @SkuType itemType: String) {
        querySkuDetailsAsync(itemType, listOf(skuId), SkuDetailsResponseListener { responseCode, skuDetailsList ->
            if (responseCode.responseCode == BillingResponseCode.OK) {
                executeServiceRequest(Runnable {
                    val purchaseParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetailsList.first())
//                            .setDeveloperId()
                    billingClient.launchBillingFlow(activity, purchaseParams.build())
                })
            }
        })
    }

    /**
     * Clear the resources
     */
    fun destroy() {
        Log.d(TAG, "Destroying the manager.")
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun querySkuDetailsAsync(@SkuType itemType: String, skuList: List<String>,
                             listener: SkuDetailsResponseListener) {
        // Creating a runnable from the request to use it inside our connection retry policy below
        val queryRequest = Runnable {
            // Query the purchase async
            val params = SkuDetailsParams.newBuilder()
                    .setSkusList(skuList)
                    .setType(itemType)
            billingClient.querySkuDetailsAsync(params.build(), listener)
        }

        executeServiceRequest(queryRequest)
    }

    fun consumeAsync(purchaseToken: String) {
        // If we've already scheduled to consume this token - no action is needed (this could happen
        // if you received the token when querying purchase items inside onReceive() and later from
        // onActivityResult()
        if (tokensToBeConsumed == null) {
            tokensToBeConsumed = HashSet()
        } else if (tokensToBeConsumed!!.contains(purchaseToken)) {
            Log.i(TAG, "Token was already scheduled to be consumed - skipping...")
            return
        }
        tokensToBeConsumed!!.add(purchaseToken)

        // Generating Consume Response listener
        val onConsumeListener = ConsumeResponseListener { billingResult, purchaseToken1 ->
            // If billing service was disconnected, we try to reconnect 1 time
            // (feel free to introduce your retry policy here).
            billingUpdatesListener.onConsumeFinished(purchaseToken1, billingResult.responseCode)
        }

        // Creating a runnable from the request to use it inside our connection retry policy below
        val consumeRequest = Runnable {
            // Consume the purchase async
            billingClient.consumeAsync(ConsumeParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build(), onConsumeListener)
        }
        executeServiceRequest(consumeRequest)
    }

    /**
     * Handles the purchase
     *
     * Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See [Security.verifyPurchase]
     *
     * @param purchase Purchase to be handled
     */
    private fun handlePurchase(purchase: Purchase) {
        if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
            Log.i(TAG, "Got a purchase: $purchase; but signature is bad. Skipping...")
            return
        }

        Log.d(TAG, "Got a verified purchase: $purchase")

        purchases.add(purchase)
    }

    /**
     * Handle a result from querying of purchase items and report an updated list to the listener
     */
    private fun onQueryPurchasesFinished(result: PurchasesResult) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "Billing client was null or result code (${result.responseCode}) was bad - quitting")
            return
        }

        Log.d(TAG, "Query inventory was successful.")

        // Update the UI and purchase items inventory with new list of purchases
        purchases.clear()
        onPurchasesUpdated(BillingResult.newBuilder()
                .setResponseCode(BillingResponseCode.OK).build(),
                result.purchasesList)
    }

    /**
     * Checks if subscriptions are supported for current client
     *
     * Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     *
     */
    fun areSubscriptionsSupported(): Boolean {
        val responseCode = billingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).responseCode
        if (responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "areSubscriptionsSupported() got an error response: $responseCode")
        }
        return responseCode == BillingResponseCode.OK
    }

    /**
     * Query purchase items across various use cases and deliver the result in a formalized way through
     * a listener
     */
    fun queryPurchases() {
        val queryToExecute = Runnable {
            val time = System.currentTimeMillis()
            val purchasesResult = billingClient.queryPurchases(SkuType.INAPP)
            Log.i(TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time)
                    + "ms")
            // If there are subscriptions supported, we add subscription rows as well
            if (areSubscriptionsSupported()) {
                val subscriptionResult = billingClient.queryPurchases(SkuType.SUBS)
                Log.i(TAG, "Querying purchases and subscriptions elapsed time: "
                        + (System.currentTimeMillis() - time) + "ms")
                Log.i(TAG, "Querying subscriptions result code: "
                        + subscriptionResult.responseCode
                        + " res: " + subscriptionResult.purchasesList.size)

                if (subscriptionResult.responseCode == BillingResponseCode.OK) {
                    purchasesResult.purchasesList.addAll(
                            subscriptionResult.purchasesList)
                } else {
                    Log.e(TAG, "Got an error response trying to query subscription purchases")
                }
            } else if (purchasesResult.responseCode == BillingResponseCode.OK) {
                Log.i(TAG, "Skipped subscription purchases query since they are not supported")
            } else {
                Log.w(TAG, "queryPurchases() got an error response code: " + purchasesResult.responseCode)
            }
            onQueryPurchasesFinished(purchasesResult)
        }

        executeServiceRequest(queryToExecute)
    }

    fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                billingClientResponseCode = result.responseCode
                Log.d(TAG, "Setup finished. Response code: $billingClientResponseCode")
                if (billingClientResponseCode == BillingResponseCode.OK) {
                    isServiceConnected = true
                    executeOnSuccess?.run()
                }
            }

            override fun onBillingServiceDisconnected() {
                billingClientResponseCode = BillingResponseCode.SERVICE_DISCONNECTED
                isServiceConnected = false
            }
        })
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected) {
            runnable.run()
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable)
        }
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     *
     * Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     */
    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            Security.verifyPurchase(BuildConfig.BASE_64_ENCODED_PUBLIC_KEY, signedData, signature)
        } catch (e: IOException) {
            Log.e(TAG, "Got an exception trying to validate a purchase: $e")
            false
        }
    }

    companion object {
        // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
        const val BILLING_MANAGER_NOT_INITIALIZED = -1

        private const val TAG = "BillingManager"
    }
}

