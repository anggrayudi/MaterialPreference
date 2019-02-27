package com.anggrayudi.materialpreference.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.anggrayudi.materialpreference.sample.billing.BillingManager
import com.anggrayudi.materialpreference.sample.billing.DonationItem

class DonationActivity : AppCompatActivity(), BillingManager.BillingUpdatesListener {

    private var mBillingManager: BillingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donation)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mBillingManager = BillingManager(this, this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DonationAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.donation, menu)
        menu.findItem(R.id.action_donate_paypal).intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TGPGSY66LKUMN&source=url"))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isFinishing) {
            mBillingManager?.destroy()
        }
    }

    override fun onBillingClientSetupFinished() {
        if (mBillingManager!!.billingClientResponseCode != BillingClient.BillingResponse.OK) {
            requestedOrientation = resources.configuration.orientation
            MaterialDialog(this)
                    .cancelable(false)
                    .message(text = "Google Play is not setup correctly")
                    .positiveButton(android.R.string.ok) {finish()}
                    .show()
        }
    }

    override fun onConsumeFinished(token: String, result: Int) {
        if (result == BillingClient.BillingResponse.OK) {
            MaterialDialog(this)
                    .title(text = "Thank you")
                    .message(text = "We have received your donation. All donations you gave are really helpful for us to develop this library.")
                    .positiveButton(android.R.string.ok)
                    .show()
        }
    }

    override fun onPurchasesUpdated(purchases: List<Purchase>) {
        purchases.forEach { mBillingManager!!.consumeAsync(it.purchaseToken) }
    }

    private inner class DonationAdapter : RecyclerView.Adapter<DonationAdapter.ViewHolder>(), View.OnClickListener {

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.itemTitle)
            val price: TextView = itemView.findViewById(R.id.itemPrice)
            val btnDonate: Button = itemView.findViewById(R.id.btnDonate)
            init {
                btnDonate.setOnClickListener(this@DonationAdapter)
            }
        }

        override fun onClick(v: View) {
            if (mBillingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK) {
                val sku = v.tag.toString()
                mBillingManager!!.initiatePurchaseFlow(sku, BillingClient.SkuType.INAPP)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_donation, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = DonationItem.items[position]
            holder.title.text = item.title
            holder.price.text = item.price
            holder.btnDonate.tag = item.sku
        }

        override fun getItemCount() = DonationItem.items.size
    }
}
