package com.bitshares.bitshareswallet;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bitshares.bitshareswallet.viewmodel.SendViewModel;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.common.ErrorCode;
import com.bitshares.bitshareswallet.wallet.exception.ErrorCodeException;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.fc.crypto.sha256_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;
import com.bituniverse.utils.NumericUtil;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SendFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendFragment extends BaseFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private KProgressHUD mProcessHud;
    private Spinner mSpinner;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    @BindView(R.id.editTextTo) EditText mEditTextTo;
    @BindView(R.id.editTextQuantity) EditText mEditTextQuantitiy;

    private View mView;
    private Handler mHandler = new Handler();

    public SendFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SendFragment newInstance(String param1, String param2) {
        SendFragment fragment = new SendFragment();
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
        mView = inflater.inflate(R.layout.fragment_send, container, false);
        ButterKnife.bind(this, mView);

        EditText editTextFrom = (EditText)mView.findViewById(R.id.editTextFrom);
        String strId = String.format(
                Locale.ENGLISH, "%d",
                BitsharesWalletWraper.getInstance().get_account().id.get_instance()
        );
        editTextFrom.setText(strId);

        mProcessHud = KProgressHUD.create(getActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please Wait")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        mView.findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processSendClick(mView);
            }
        });



        mEditTextTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                sha256_object.encoder encoder = new sha256_object.encoder();
                encoder.write(s.toString().getBytes());
            }
        });


        mEditTextQuantitiy.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    return;
                }

                String strQuantity = mEditTextQuantitiy.getText().toString();
                if (TextUtils.isEmpty(strQuantity)) {
                    return;
                }

                double quantity = NumericUtil.parseDouble(strQuantity, -1.0D);
                if (Double.compare(quantity, 0.0D) < 0) {   // 越界或者格式错误
                    mEditTextQuantitiy.setText("0");
                    // TODO toast
                    return;
                }

                processCalculateFee();
            }
        });

        mSpinner = (Spinner) mView.findViewById(R.id.spinner_unit);

        SendViewModel viewModel = ViewModelProviders.of(this).get(SendViewModel.class);
        viewModel.getBalancesList().observe(this, bitsharesBalanceAssetList -> {
            List<String> symbolList = new ArrayList<>();
            for (BitsharesBalanceAsset bitsharesBalanceAsset : bitsharesBalanceAssetList) {
                symbolList.add(bitsharesBalanceAsset.quote);

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        getActivity(),
                        android.R.layout.simple_spinner_item,
                        symbolList
                );

                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinner.setAdapter(arrayAdapter);
            }
        });

        return mView;
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
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onShow() {
        super.onShow();

        notifyUpdate();
    }

    @Override
    public void onHide() {
        super.onHide();
        hideSoftKeyboard(mEditTextTo, getActivity());
    }

    private void processTransfer(final String strFrom,
                                 final String strTo,
                                 final String strQuantity,
                                 final String strSymbol,
                                 final String strMemo) {
        mProcessHud.show();
        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    signed_transaction signedTransaction = BitsharesWalletWraper.getInstance().transfer(
                            strFrom,
                            strTo,
                            strQuantity,
                            strSymbol,
                            strMemo
                    );
                    return signedTransaction;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(signedTransaction -> {
                    if (getActivity() != null && getActivity().isFinishing() == false) {
                        mProcessHud.dismiss();
                        if (signedTransaction != null) {
                            Toast.makeText(getActivity(), "Sent Successfully", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), "Fail to send", Toast.LENGTH_LONG).show();
                        }
                    }

                }, throwable -> {
                    if (throwable instanceof NetworkStatusException) {
                        throwable.printStackTrace();
                        if (getActivity() != null && getActivity().isFinishing() == false) {
                            mProcessHud.dismiss();
                            Toast.makeText(getActivity(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        throw Exceptions.propagate(throwable);
                    }
                });
    }

    private void processSendClick(final View view) {
        if (BitsharesWalletWraper.getInstance().is_locked()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            final View viewGroup = layoutInflater.inflate(R.layout.dialog_password_confirm, null);
            builder.setPositiveButton(
                    R.string.password_confirm_button_confirm,
                    null);

            builder.setNegativeButton(
                    R.string.password_confirm_button_cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }
            );
            builder.setView(viewGroup);
            final AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editText = (EditText) viewGroup.findViewById(R.id.editTextPassword);
                    String strPassword = editText.getText().toString();
                    int nRet = BitsharesWalletWraper.getInstance().unlock(strPassword);
                    if (nRet == 0) {
                        dialog.dismiss();
                        String strFrom = ((EditText) view.findViewById(R.id.editTextFrom)).getText().toString();
                        String strTo = ((EditText) view.findViewById(R.id.editTextTo)).getText().toString();
                        String strQuantity = ((EditText) view.findViewById(R.id.editTextQuantity)).getText().toString();
                        String strSymbol = (String)mSpinner.getSelectedItem();
                        String strMemo = "";
                        processTransfer(strFrom, strTo, strQuantity, strSymbol, strMemo);
                    } else {
                        viewGroup.findViewById(R.id.textViewPasswordInvalid).setVisibility(View.VISIBLE);
                    }
                }
            });

        } else {
            String strFrom = ((EditText) view.findViewById(R.id.editTextFrom)).getText().toString();
            String strTo = ((EditText) view.findViewById(R.id.editTextTo)).getText().toString();
            String strQuantity = ((EditText) view.findViewById(R.id.editTextQuantity)).getText().toString();
            String strSymbol = (String)mSpinner.getSelectedItem();
            String strMemo = "";

            processTransfer(strFrom, strTo, strQuantity, strSymbol, strMemo);
        }
    }

    private void processGetTransferToId(final String strAccount, final TextView textViewTo) {
        Flowable.just(strAccount)
                .subscribeOn(Schedulers.io())
                .map(accountName -> {
                    account_object accountObject = BitsharesWalletWraper.getInstance().get_account_object(accountName);
                    if (accountObject == null) {
                        throw new ErrorCodeException(ErrorCode.ERROR_NO_ACCOUNT_OBJECT, "it can't find the account");
                    }

                    return accountObject;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(accountObject -> {
                    if (getActivity() != null && getActivity().isFinishing() == false) {
                        textViewTo.setText("#" + accountObject.id.get_instance());
                    }
                }, throwable -> {
                    if (throwable instanceof NetworkStatusException || throwable instanceof ErrorCodeException) {
                        if (getActivity() != null && getActivity().isFinishing() == false) {
                            textViewTo.setText("#none");
                        }
                    } else {
                        throw Exceptions.propagate(throwable);
                    }
                });
    }



    public static void hideSoftKeyboard(View view, Context context) {
        if (view != null && context != null) {
            InputMethodManager imm = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public static void showSoftKeyboard(View view, Context context) {
        if (view != null && context != null) {
            InputMethodManager imm = (InputMethodManager) context
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }
    }

    @Override
    public void notifyUpdate() {

    }

    private void processCalculateFee() {
        final String strQuantity = ((EditText) mView.findViewById(R.id.editTextQuantity)).getText().toString();
        final String strSymbol = (String) mSpinner.getSelectedItem();
        final String strMemo = "";

        // 用户没有任何货币，这个symbol会为空，则会出现崩溃，进行该处理进行规避
        if (TextUtils.isEmpty(strQuantity) || TextUtils.isEmpty(strSymbol)) {
            return;
        }

        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    asset fee = BitsharesWalletWraper.getInstance().transfer_calculate_fee(
                            strQuantity,
                            strSymbol,
                            strMemo
                    );

                    BitsharesAssetObject assetObject = BitsharesApplication.getInstance()
                            .getBitsharesDatabase().getBitsharesDao().queryAssetObjectById(fee.asset_id.toString());
                    return new Pair<>(fee, assetObject);
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(resultPair -> {
                    if (getActivity() != null && getActivity().isFinishing() == false) {
                        processDisplayFee(resultPair.first, resultPair.second);
                    }
                }, throwable -> {
                    if (throwable instanceof NetworkStatusException) {
                        if (getActivity() != null && getActivity().isFinishing() == false) {
                            EditText editTextFee = (EditText) mView.findViewById(R.id.editTextFee);
                            editTextFee.setText("N/A");
                        }
                    } else {
                        throw Exceptions.propagate(throwable);
                    }
                });

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    asset fee = BitsharesWalletWraper.getInstance().transfer_calculate_fee(
                            strQuantity,
                            strSymbol,
                            strMemo
                    );

                    BitsharesAssetObject assetObject = BitsharesApplication.getInstance()
                            .getBitsharesDatabase().getBitsharesDao().queryAssetObjectById(fee.asset_id.toString());



                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && getActivity().isFinishing() == false) {
                                processDisplayFee(legibleObject);
                            }
                        }
                    });
                } catch (NetworkStatusException e) {
                    e.printStackTrace();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && getActivity().isFinishing() == false) {
                                EditText editTextFee = (EditText) mView.findViewById(R.id.editTextFee);
                                editTextFee.setText("N/A");
                            }
                        }
                    });
                }
            }
        }).start();*/
    }

    private void processDisplayFee(asset fee, BitsharesAssetObject assetObject) {
        EditText editTextFee = (EditText) mView.findViewById(R.id.editTextFee);
        String strResult = String.format(
                Locale.ENGLISH,
                "%f (%s)",
                (double)fee.amount / assetObject.precision,
                "Cannot be modified"
        );
        editTextFee.setText(strResult);
    }

    private void loadWebView(WebView webView, int size, String encryptText) {
        String htmlShareAccountName = "<html><head><style>body,html {margin:0; padding:0; text-align:center;}</style><meta name=viewport content=width=" + size + ",user-scalable=no/></head><body><canvas width=" + size + " height=" + size + " data-jdenticon-hash=" + encryptText + "></canvas><script src=https://cdn.jsdelivr.net/jdenticon/1.3.2/jdenticon.min.js async></script></body></html>";
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadData(htmlShareAccountName, "text/html", "UTF-8");
    }

    public void processDonate(String strName, int nAmount, String strUnit) {
        if (isAdded()) {
            mEditTextTo.setText(strName);
            mEditTextQuantitiy.setText(Integer.toString(nAmount));
            mSpinner.setSelection(0);
        }
    }
}
