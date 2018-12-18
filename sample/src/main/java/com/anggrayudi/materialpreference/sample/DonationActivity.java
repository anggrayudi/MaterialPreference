package com.anggrayudi.materialpreference.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.anggrayudi.materialpreference.sample.billing.BillingManager;
import com.anggrayudi.materialpreference.sample.billing.Constants;
import com.anggrayudi.materialpreference.sample.billing.DonationItem;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DonationActivity extends AppCompatActivity implements BillingManager.BillingUpdatesListener {

    private BillingManager mBillingManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBillingManager = new BillingManager(this, this);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DonationAdapter());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isFinishing()) {
            mBillingManager.destroy();
        }
    }

    @Override
    public void onBillingClientSetupFinished() {
        if (mBillingManager.mBillingClientResponseCode != BillingClient.BillingResponse.OK) {
            setRequestedOrientation(getResources().getConfiguration().orientation);
            new MaterialDialog.Builder(this)
                    .cancelable(false)
                    .content("Google Play is not setup correctly")
                    .positiveText(android.R.string.ok)
                    .onPositive((dialog, which) -> finish())
                    .show();
        }
    }

    @Override
    public void onConsumeFinished(String token, int result) {
        if (result == BillingClient.BillingResponse.OK) {
            new MaterialDialog.Builder(this)
                    .title("Thank you")
                    .content("We have received your donation. All donations you gave are really helpful for us to develop this library.")
                    .positiveText(android.R.string.ok)
                    .show();
        }
    }

    @Override
    public void onPurchasesUpdated(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            mBillingManager.consumeAsync(purchase.getPurchaseToken());
        }
    }

    private class DonationAdapter extends RecyclerView.Adapter<DonationAdapter.ViewHolder> implements
            View.OnClickListener {

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView title, price, description;
            final Button btnDonate;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.itemTitle);
                price = itemView.findViewById(R.id.itemPrice);
                description = itemView.findViewById(R.id.description);
                btnDonate = itemView.findViewById(R.id.btnDonate);
                btnDonate.setOnClickListener(DonationAdapter.this);
            }
        }

        @Override
        public void onClick(View v) {
            if (mBillingManager.mBillingClientResponseCode == BillingClient.BillingResponse.OK) {
                String sku = v.getTag().toString();
                mBillingManager.initiatePurchaseFlow(sku, BillingClient.SkuType.INAPP);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_donation, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DonationItem item = Constants.DONATION_ITEMS[position];
            holder.title.setText(item.title);
            holder.price.setText(item.price);
            holder.description.setText(item.description);
            holder.btnDonate.setTag(item.sku);
        }

        @Override
        public int getItemCount() {
            return Constants.DONATION_ITEMS.length;
        }
    }
}
