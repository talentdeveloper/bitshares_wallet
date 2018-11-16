package com.bitshares.bitshareswallet;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bitshares.bitshareswallet.market.MarketTicker;
import com.bitshares.bitshareswallet.viewmodel.QuotationViewModel;

import butterknife.BindView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ExchangeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ExchangeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExchangeFragment extends BaseFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private QuotationCurrencyPairAdapter quotationCurrencyPairAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private LinearLayout mLayoutTitle;
    private TextView mTxtTitle;
    private BtsFragmentPageAdapter mExchangeFragmentPageAdapter;

    private OrdersFragment mOrdersFragment;
    public ExchangeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ExchangeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ExchangeFragment newInstance(String param1, String param2) {
        ExchangeFragment fragment = new ExchangeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_exchange, container, false);
        mTabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        mViewPager = (ViewPager) view.findViewById(R.id.exchangeViewPager);
        mLayoutTitle = (LinearLayout)view.findViewById(R.id.lay_title);
        mExchangeFragmentPageAdapter = new BtsFragmentPageAdapter(getFragmentManager());
        mExchangeFragmentPageAdapter.addFragment(TransactionSellBuyFragment.newInstance(TransactionSellBuyFragment.TRANSACTION_BUY), getResources().getString(R.string.label_buy));
        mExchangeFragmentPageAdapter.addFragment(TransactionSellBuyFragment.newInstance(TransactionSellBuyFragment.TRANSACTION_SELL), getResources().getString(R.string.label_sell));
        mOrdersFragment = OrdersFragment.newInstance();
        mExchangeFragmentPageAdapter.addFragment(mOrdersFragment, getResources().getString(R.string.title_my_orders));
        mViewPager.setAdapter(mExchangeFragmentPageAdapter);
        initPager(mViewPager, mExchangeFragmentPageAdapter);

        mTxtTitle = (TextView) mLayoutTitle.findViewById(R.id.txt_bar_title);
        String value = (String) ((MainActivity)getActivity()).mTxtTitle.getText();
        mTxtTitle.setText(value.replaceAll("ZMKZM","ZMK"));
        mLayoutTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).processChooseCurency();
            }
        });

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() != 2) {
                    MainActivity.hideSoftKeyboard(mTabLayout, getActivity());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mTabLayout.setupWithViewPager(mViewPager);
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onShow() {
        super.onShow();

        QuotationViewModel viewModel = ViewModelProviders.of(getActivity()).get(QuotationViewModel.class);
        viewModel.getSelectedMarketTicker().observe(this, currencyPair -> {
            String value = (String) ((MainActivity)getActivity()).mTxtTitle.getText();
            mTxtTitle.setText(value.replaceAll("ZMKZM","ZMK"));
        });
    }
}
