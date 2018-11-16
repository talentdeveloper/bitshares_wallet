package com.bitshares.bitshareswallet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.common.ErrorCode;
import com.kaopiz.kprogresshud.KProgressHUD;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SignUpButtonActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private KProgressHUD mProcessHud;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_button);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mToolbar.setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mProcessHud = KProgressHUD.create(SignUpButtonActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please Wait")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        findViewById(R.id.sign_up_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpButtonActivity.this, CreateAccountActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessHud.show();
                String strAccount = ((EditText)findViewById(R.id.editTextAccountName)).getText().toString();
                String strPassword = ((EditText)findViewById(R.id.editTextPassword)).getText().toString();
                processImport(strAccount, strPassword);
            }
        });
    }
    private void processImport(final String strAccount,
                               final String strPassword) {
        final TextView textView = (TextView) findViewById(R.id.textViewErrorInfo);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int nRet = BitsharesWalletWraper.getInstance().build_connect();
                if (nRet != 0) {
                    // // TODO: 01/09/2017 连接失败的处理
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProcessHud.dismiss();
                            textView.setText(R.string.import_activity_connect_failed);
                        }
                    });
                    return;
                }
                nRet = BitsharesWalletWraper.getInstance().import_account_password(
                        strAccount,
                        strPassword
                );
                if (nRet == 0) {
                    mProcessHud.dismiss();
                    Intent intent = new Intent(SignUpButtonActivity.this, MainActivity.class);
                    intent.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    processErrorCode(nRet);
                }
            }
        }).start();
    }

    private void processErrorCode(final int nRet) {
        final TextView textView = (TextView) findViewById(R.id.textViewErrorInfo);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProcessHud.dismiss();
                if (nRet == ErrorCode.ERROR_FILE_NOT_FOUND) {
                    textView.setText(R.string.import_activity_file_failed);
                } else if (nRet == ErrorCode.ERROR_FILE_READ_FAIL) {
                    textView.setText(R.string.import_activity_file_failed);
                } else if (nRet == ErrorCode.ERROR_NO_ACCOUNT_OBJECT) {
                    textView.setText(R.string.import_activity_account_name_invalid);
                } else if (nRet == ErrorCode.ERROR_IMPORT_NOT_MATCH_PRIVATE_KEY) {
                    textView.setText(R.string.import_activity_private_key_invalid);
                } else if (nRet == ErrorCode.ERROR_NETWORK_FAIL) {
                    textView.setText(R.string.import_activity_connect_failed);
                } else if (nRet == ErrorCode.ERROR_PASSWORD_INVALID) {
                    textView.setText(R.string.import_activity_password_invalid);
                } else if (nRet == ErrorCode.ERROR_FILE_BIN_PASSWORD_INVALID) {
                    textView.setText(R.string.import_activity_file_bin_password_invalid);
                } else if (nRet == ErrorCode.ERROR_UNKNOWN) {
                    textView.setText(R.string.import_activity_unknown_error);
                }
            }
        });
    }
}
