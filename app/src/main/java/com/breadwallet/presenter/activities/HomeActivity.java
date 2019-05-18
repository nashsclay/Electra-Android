/**
 * BreadWallet
 * <p/>
 * Created by byfieldj on <jade@breadwallet.com> 1/17/18.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.breadwallet.presenter.activities;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.model.Wallet;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BRSearchBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.presenter.viewmodels.MainViewModel;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    public static final String EXTRA_DATA = "com.breadwallet.presenter.activities.WalletActivity.EXTRA_DATA";


    private WalletListAdapter mAdapter;
    private BRNotificationBar mNotificationBar;
    private LinearLayout mMenuLayout;
    private MainViewModel mViewModel;
    private BRButton mSendButton;
    private BRButton mReceiveButton;
    public static final String EXTRA_CRYPTO_REQUEST ="com.breadwallet.presenter.activities.WalletActivity.EXTRA_CRYPTO_REQUEST";
    private static final int SEND_SHOW_DELAY = 300;
    private BaseTextView mCurrencyPriceUsd;
    private BaseTextView mBalancePrimary;
    private BaseTextView mBalanceSecondary;
    private BRSearchBar mSearchBar;
    public ViewFlipper mBarFlipper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ImageButton searchIcon = findViewById(R.id.search_icon);
        mNotificationBar = findViewById(R.id.notification_bar);
        mMenuLayout = findViewById(R.id.menu_layout);
        mMenuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SETTINGS);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });


        processIntentData(getIntent());

        mAdapter = new WalletListAdapter(this);

        // Get ViewModel, observe updates to Wallet and aggregated balance data
        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        mViewModel.getWallets().observe(this, new Observer<List<Wallet>>() {
            @Override
            public void onChanged(@Nullable List<Wallet> wallets) {
                mAdapter.setWallets(wallets);
                for (Wallet wallet : wallets) {
                    mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
                    BigDecimal rate = wallet.getExchangeRate();

                    rate = rate.setScale(8, RoundingMode.HALF_EVEN);

                    mCurrencyPriceUsd.setText(String.format(getString(R.string.Account_exchangeRate),
                            rate.toPlainString(), wallet.getCurrencyCode()));

                    mBalancePrimary = findViewById(R.id.balance_primary);

                    String fiatBalance = wallet.getFiatBalance().toPlainString();

                    mBalancePrimary.setText(fiatBalance);

                    mBalanceSecondary = findViewById(R.id.balance_secondary);

                    String cryptoBalance = wallet.getCryptoBalance().toPlainString();

                    mBalanceSecondary.setText(cryptoBalance);

                }
            }
        });

        mSendButton = findViewById(R.id.send_button);
        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSendFragment(null);
            }
        });

        mSendButton.setHasShadow(false);
        mReceiveButton = findViewById(R.id.receive_button);
        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.showReceiveFragment(HomeActivity.this, true);
            }
        });
        mSearchBar = findViewById(R.id.search_bar);
        mBarFlipper = findViewById(R.id.tool_bar_flipper);
        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                mBarFlipper.setDisplayedChild(1); //search bar
                mSearchBar.onShow(true);
            }
        });

        //updateUi();

    }
    @Override
    protected void onStart() {
        super.onStart();
        //updateUi();
    }
    public void resetFlipper() {
        mBarFlipper.setDisplayedChild(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntentData(intent);
    }

    private synchronized void processIntentData(Intent intent) {
        String data = intent.getStringExtra(EXTRA_DATA);
        if (Utils.isNullOrEmpty(data)) {
            data = intent.getDataString();
        }
        if (data != null) {
            AppEntryPointHandler.processDeepLink(this, data);
        }
    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.nextPrompt(this);
        if (toShow != null) {
            View promptView = PromptManager.promptInfo(this, toShow);

            EventUtils.pushEvent(EventUtils.EVENT_PROMPT_PREFIX
                    + PromptManager.getPromptName(toShow) + EventUtils.EVENT_PROMPT_SUFFIX_DISPLAYED);
        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNextPromptIfNeeded();
        InternetManager.registerConnectionReceiver(this, this);
        onConnectionChanged(InternetManager.getInstance().isConnected(this));
        mViewModel.refreshWallets();
    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.GONE);
            }
        } else {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.bringToFront();
            }
        }
    }

    public void closeNotificationBar() {
        mNotificationBar.setVisibility(View.INVISIBLE);
    }

    public void showSendFragment(final CryptoRequest request) {
        // TODO: Find a better solution.
        if (FragmentSend.isIsSendShown()) {
            return;
        }
        FragmentSend.setIsSendShown(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentSend fragmentSend = (FragmentSend) getSupportFragmentManager()
                        .findFragmentByTag(FragmentSend.class.getName());
                if (fragmentSend == null) {
                    fragmentSend = new FragmentSend();
                }

                Bundle arguments = new Bundle();
                arguments.putSerializable(EXTRA_CRYPTO_REQUEST, request);
                fragmentSend.setArguments(arguments);
                if (!fragmentSend.isAdded()) {
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                            .add(android.R.id.content, fragmentSend, FragmentSend.class.getName())
                            .addToBackStack(FragmentSend.class.getName()).commit();
                }
            }
        }, SEND_SHOW_DELAY);

    }
}
