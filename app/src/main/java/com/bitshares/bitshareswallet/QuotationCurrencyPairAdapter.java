package com.bitshares.bitshareswallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.room.BitsharesMarketTicker;
import com.bitshares.bitshareswallet.wallet.graphene.chain.utils;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by lorne on 22/09/2017.
 */

public class QuotationCurrencyPairAdapter extends RecyclerView.Adapter<QuotationCurrencyPairAdapter.ViewHolder> {
    public interface OnItemClickListner {
        void onItemClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View mView;
        private ImageView mCurrencyIconView;
        private ImageView mStatus;
        private TextView mViewCurrencyPair;
        private TextView mBottomPrice;
        private TextView mTopPrice;
        private TextView mUnit;
        private TextView mView24h;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mCurrencyIconView = (ImageView) mView.findViewById(R.id.imageViewCurrency);
            mViewCurrencyPair = (TextView) mView.findViewById(R.id.textViewCurrencyPair);
            mBottomPrice = (TextView) mView.findViewById(R.id.textViewPrice);
            mView24h = (TextView) mView.findViewById(R.id.textView24h);
            mStatus = (ImageView)mView.findViewById(R.id.imageViewStatus);
            mUnit = (TextView)mView.findViewById(R.id.textViewUnit);
            mTopPrice = (TextView)mView.findViewById(R.id.textViewTotal);
        }
    }

    private String[] marrOptions;
    private String[] marrValues;
    private Context mContext;
    private OnItemClickListner monItemClickListner;
    private Map<String, Integer> mapSymbol2Id = new HashMap<>();
    private List<BitsharesMarketTicker> bitsharesMarketTickerList;
    private Set<String> currecnyPairSet = new HashSet<>();
    private int selected = 0;
    private double zmk_usd_latest = 0;

    public QuotationCurrencyPairAdapter(Context context) {
        mContext = context;
        marrOptions = context.getResources().getStringArray(R.array.quotation_currency_pair_options);
        marrValues = context.getResources().getStringArray(R.array.quotation_currency_pair_values);
        currecnyPairSet.addAll(Arrays.asList(marrValues));
        mapSymbol2Id.put("ZMKZM", R.mipmap.zmk);
        mapSymbol2Id.put("GRZBND", R.mipmap.grzbnd);
        mapSymbol2Id.put("FROSCU", R.mipmap.froscu);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item_currency_pair, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        BitsharesMarketTicker bitsharesMarketTicker = bitsharesMarketTickerList.get(position);
        MarketTicker marketTicker = bitsharesMarketTicker.marketTicker;
        String currencyPair = utils.getAssetSymbolDisply(marketTicker.quote) + " : " +
                utils.getAssetSymbolDisply(marketTicker.base);
        holder.mUnit.setText(utils.getAssetSymbolDisply(marketTicker.quote).replaceAll("ZMKZM","ZMK"));
        String description = "";
        switch (marketTicker.quote)
        {
            case "ZMKZM":description = "Zambian Kwacha";break;
            case "GRZBND":description = "2Y GRZ Bond 14%";break;
            case "FROSCU":description = "FROSCU SHARE";break;
            default:description = holder.mUnit.getText().toString();break;
        }
        holder.mUnit.setText(description);
        holder.mViewCurrencyPair.setText(currencyPair.replaceAll("ZMKZM","ZMK"));
        Integer nId = mapSymbol2Id.get(utils.getAssetSymbolDisply(marketTicker.quote));
        if (nId != null) {
            holder.mCurrencyIconView.setImageResource(nId);
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.####");
        if(marketTicker.quote.equals("ZMKZM"))
        {
            holder.mBottomPrice.setText("1 ZMK");
            holder.mTopPrice.setText("$ "+ decimalFormat.format(marketTicker.latest));
        }
        else if(marketTicker.base.equals("USD"))
        {
            holder.mTopPrice.setText("$ "+ decimalFormat.format(marketTicker.latest));
            double value = (this.zmk_usd_latest == 0)?0:marketTicker.latest / this.zmk_usd_latest;
            holder.mBottomPrice.setText(decimalFormat.format(value) +" ZMK");
        }
        else{
            holder.mTopPrice.setText(decimalFormat.format(marketTicker.latest)+" ZMK");
            holder.mBottomPrice.setText("$ "+decimalFormat.format(marketTicker.latest * this.zmk_usd_latest ));
        }

        double percent_change = 0.f;
        try {
            percent_change = Double.parseDouble(marketTicker.percent_change);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String strPercentChange;
        if (percent_change >= 0) {
            if (!MainActivity.rasingColorRevers) {
                holder.mView24h.setTextColor(mContext.getResources().getColor(R.color.quotation_top_green));
            } else {
                holder.mView24h.setTextColor(mContext.getResources().getColor(R.color.quotation_top_red));
            }
            strPercentChange = String.format(
                    Locale.ENGLISH,
                    "+%.2f%%",
                    percent_change
            );
            holder.mStatus.setImageResource(R.mipmap.ic_plus);
        } else {
            if (!MainActivity.rasingColorRevers) {
                holder.mView24h.setTextColor(mContext.getResources().getColor(R.color.quotation_top_red));
            } else {
                holder.mView24h.setTextColor(mContext.getResources().getColor(R.color.quotation_top_green));
            }
            strPercentChange = String.format(
                    Locale.ENGLISH,
                    "%.2f%%",
                    percent_change
            );
            holder.mStatus.setImageResource(R.mipmap.ic_minus);
        }
        holder.mView24h.setText(strPercentChange);


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (monItemClickListner != null) {
                    selected = position;
                    monItemClickListner.onItemClick(holder.mView, position);
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (bitsharesMarketTickerList == null) {
            return 0;
        } else {
            return bitsharesMarketTickerList.size();
        }
    }

    public void setOnItemClickListenr(OnItemClickListner onItemClickListenr) {
        monItemClickListner = onItemClickListenr;
    }

    public void notifyDataUpdated(List<BitsharesMarketTicker> marketTickerList) {
        bitsharesMarketTickerList = new ArrayList<>();
        for (BitsharesMarketTicker bitsharesMarketTicker : marketTickerList) {
            String str = bitsharesMarketTicker.marketTicker.quote + ":" + bitsharesMarketTicker.marketTicker.base;
            if(str.equals("ZMKZM:USD"))
            {
                this.zmk_usd_latest = bitsharesMarketTicker.marketTicker.latest;
               // Toast.makeText(mContext,"latest:"+this.zmk_usd_latest,(int)10).show();
            }
            if (currecnyPairSet.contains(str)) {
                bitsharesMarketTickerList.add(bitsharesMarketTicker);
            }
        }

        Collections.sort(
                bitsharesMarketTickerList,
                (o1, o2) -> o1.marketTicker.quote.compareTo(o2.marketTicker.quote)
        );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String strAssetPairConfig = prefs.getString("quotation_currency_pair", "BTS:USD");
        for (int i = 0; i < bitsharesMarketTickerList.size(); ++i) {
            MarketTicker marketTicker = bitsharesMarketTickerList.get(i).marketTicker;
            String currencyPair = utils.getAssetSymbolDisply(marketTicker.quote) + ":" +
                    utils.getAssetSymbolDisply(marketTicker.base);
            if (strAssetPairConfig.compareTo(currencyPair) == 0) {
                selected = i;
                break;
            }
        }

        notifyDataSetChanged();
    }

    public BitsharesMarketTicker getSelectedMarketTicker() {
        if (selected >= bitsharesMarketTickerList.size()) {
            return null;
        }

        return bitsharesMarketTickerList.get(selected);
    }
}
