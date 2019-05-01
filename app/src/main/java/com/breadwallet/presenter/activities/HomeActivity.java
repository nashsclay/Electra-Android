/**
 * BreadWallet
 * ElectraWallet
 * <p/>
 * Copyright (c) 2019 breadwallet LLC
 * Copyright (c) 2019 Electra project team
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

import android.animation.Animator;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;							   
import android.content.Intent;
import android.os.Handler;						  
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ViewFlipper;								  
import android.widget.LinearLayout;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.model.Wallet;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRSearchBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.viewmodels.MainViewModel;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.services.SyncService;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.SyncTestLogger;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener,
        OnTxListModified, RatesDataSource.OnDataChanged, SyncListener, BalanceUpdateListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String URBAN_APP_PACKAGE_NAME = "com.urbandroid.lux";
    public static final String EXTRA_DATA = "com.breadwallet.presenter.activities.HomeActivity.EXTRA_DATA";

    public static final String EXTRA_CRYPTO_REQUEST =
            "com.breadwallet.presenter.activities.HomeActivity.EXTRA_CRYPTO_REQUEST";
    public static final int MAX_NUMBER_OF_CHILDREN = 2;

    private static final String SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm";
    private static final float SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA = 0.0f;
    private static final int SEND_SHOW_DELAY = 300;

    private WalletListAdapter mAdapter;
    private BaseTextView mCurrencyTitle;
    private BaseTextView mCurrencyPriceUsd;
    private BaseTextView mBalancePrimary;
    private BaseTextView mBalanceSecondary;
    private Toolbar mToolbar;
    private BRButton mSendButton;
    private BRButton mReceiveButton;
    private BRButton mSellButton;
    private LinearLayout mProgressLayout;
    private BaseTextView mSyncStatusLabel;
    private BaseTextView mProgressLabel;
    public ViewFlipper mBarFlipper;
    private BRSearchBar mSearchBar;
    private ConstraintLayout mToolBarConstraintLayout;
    private LinearLayout mWalletFooter;
    private View mDelistedTokenBanner;
    private BaseTextView mFiatTotal;
    private BRNotificationBar mNotificationBar;
    private LinearLayout mMenuLayout;
    private LinearLayout mListGroupLayout;
    private MainViewModel mViewModel;

    private static final float PRIMARY_TEXT_SIZE = 30;
    private static final float SECONDARY_TEXT_SIZE = 16;

    private static final boolean RUN_LOGGER = false;

    private SyncTestLogger mTestLogger;

    private HomeActivity.SyncNotificationBroadcastReceiver mSyncNotificationBroadcastReceiver;
    private String mCurrentWalletIso;

    private BaseWalletManager mWallet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
		
        BRSharedPrefs.putIsNewWallet(this, false);

        mFiatTotal = findViewById(R.id.total_assets_usd);
        mNotificationBar = findViewById(R.id.notification_bar);
        mMenuLayout = findViewById(R.id.menu_layout);
        mListGroupLayout = findViewById(R.id.list_group_layout);
        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mToolbar = findViewById(R.id.bread_bar);
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mSellButton = findViewById(R.id.sell_button);
        mBarFlipper = findViewById(R.id.tool_bar_flipper);
        mSearchBar = findViewById(R.id.search_bar);
        ImageButton searchIcon = findViewById(R.id.search_icon);
        mToolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mProgressLayout = findViewById(R.id.progress_layout);
        mSyncStatusLabel = findViewById(R.id.sync_status_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mWalletFooter = findViewById(R.id.bottom_toolbar_layout1);
        mDelistedTokenBanner = findViewById(R.id.delisted_token_layout);

        startSyncLoggerIfNeeded();

        setUpBarFlipper();

        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, PRIMARY_TEXT_SIZE);
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, SECONDARY_TEXT_SIZE);

        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSendFragment(null);
            }
        });
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
            }
        });

        mViewModel.getAggregatedFiatBalance().observe(this, new Observer<BigDecimal>() {
            @Override
            public void onChanged(@Nullable BigDecimal aggregatedFiatBalance) {
                if (aggregatedFiatBalance == null) {
                    Log.e(TAG, "fiatTotalAmount is null");
                    return;
                }
                mFiatTotal.setText(CurrencyUtils.getFormattedAmount(HomeActivity.this,
                        BRSharedPrefs.getPreferredFiatIso(HomeActivity.this), aggregatedFiatBalance));
            }
        });
    }

    /**
     * This token is no longer supported by the BRD app, notify the user.
     */
    private void showDelistedTokenBanner() {
        mDelistedTokenBanner.setVisibility(View.VISIBLE);
    }

    private void startSyncLoggerIfNeeded() {
        if (BuildConfig.DEBUG && RUN_LOGGER) {
            if (mTestLogger != null) {
                mTestLogger.interrupt();
            }
            mTestLogger = new SyncTestLogger(this); //Sync logger
            mTestLogger.start();
        }
    }

    private void setUpBarFlipper() {
        mBarFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        mBarFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    private void updateUi() {
        final BaseWalletManager walletManager = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (walletManager == null) {
            Log.e(TAG, "updateUi: wallet is null");
            return;
        }

        BigDecimal bigExchangeRate = walletManager.getFiatExchangeRate(this);

        String fiatExchangeRate = CurrencyUtils.getFormattedAmount(this,
                BRSharedPrefs.getPreferredFiatIso(this), bigExchangeRate);
        String fiatBalance = CurrencyUtils.getFormattedAmount(this,
                BRSharedPrefs.getPreferredFiatIso(this), walletManager.getFiatBalance(this));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(this,
                walletManager.getCurrencyCode(), walletManager.getCachedBalance(this),
                walletManager.getUiConfiguration().getMaxDecimalPlacesForUi());

        mCurrencyTitle.setText(walletManager.getName());
        mCurrencyPriceUsd.setText(String.format(getString(R.string.Account_exchangeRate),
                fiatExchangeRate, walletManager.getCurrencyCode()));
        mBalancePrimary.setText(fiatBalance);
        mBalanceSecondary.setText(cryptoBalance);

        if (Utils.isNullOrZero(bigExchangeRate)) {
            mCurrencyPriceUsd.setVisibility(View.INVISIBLE);
        } else {
            mCurrencyPriceUsd.setVisibility(View.VISIBLE);
        }

        String startColor = walletManager.getUiConfiguration().getStartColor();
        String endColor = walletManager.getUiConfiguration().getEndColor();
        int currentTheme = UiUtils.getThemeId(this);

        if (currentTheme == R.style.AppTheme_Dark) {
            mSendButton.setColor(getColor(R.color.wallet_footer_button_color_dark));
            mReceiveButton.setColor(getColor(R.color.wallet_footer_button_color_dark));
            mSellButton.setColor(getColor(R.color.wallet_footer_button_color_dark));

            if (endColor != null) {
                mWalletFooter.setBackgroundColor(Color.parseColor(endColor));
            }
        } else {
            if (endColor != null) {
                mSendButton.setColor(Color.parseColor(endColor));
                mReceiveButton.setColor(Color.parseColor(endColor));
                mSellButton.setColor(Color.parseColor(endColor));
            }
        }

        if (endColor != null) {
            //it's a gradient
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.parseColor(startColor), Color.parseColor(endColor)});
            gd.setCornerRadius(0f);
            mToolbar.setBackground(gd);
        } else {
            //it's a solid color
            mToolbar.setBackgroundColor(Color.parseColor(startColor));
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BG:" + TAG + ":updateTxList");
                TxManager.getInstance().updateTxList(HomeActivity.this);
            }
        });

    }

    private void setPriceTags(final boolean cryptoPreferred, boolean animate) {
        ConstraintSet set = new ConstraintSet();
        set.clone(mToolBarConstraintLayout);
        if (animate) {
            TransitionManager.beginDelayedTransition(mToolBarConstraintLayout);
        }
        int balanceTextMargin = (int) getResources().getDimension(R.dimen.balance_text_margin);
        int balancePrimaryPadding = (int) getResources().getDimension(R.dimen.balance_primary_padding);
        int balanceSecondaryPadding = (int) getResources().getDimension(R.dimen.balance_secondary_padding);
        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            set.connect(R.id.balance_secondary, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, balanceTextMargin);
            mBalancePrimary.setPadding(0, balancePrimaryPadding, 0, 0);
            mBalanceSecondary.setPadding(0, balanceSecondaryPadding, 0, 0);

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, balanceTextMargin);

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin);

            mBalanceSecondary.setTextSize(PRIMARY_TEXT_SIZE);
            mBalancePrimary.setTextSize(SECONDARY_TEXT_SIZE);

            set.applyTo(mToolBarConstraintLayout);

        } else {
            // CRYPTO on LEFT
            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID,
                    ConstraintSet.END, balanceTextMargin);

            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, balanceTextMargin);


            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin);
            mBalancePrimary.setPadding(0, balanceSecondaryPadding, 0, 0);
            mBalanceSecondary.setPadding(0, balancePrimaryPadding, 0, 0);

            mBalanceSecondary.setTextSize(SECONDARY_TEXT_SIZE);
            mBalancePrimary.setTextSize(PRIMARY_TEXT_SIZE);

            set.applyTo(mToolBarConstraintLayout);
        }
        mBalanceSecondary.setTextColor(getResources().getColor(cryptoPreferred
                ? R.color.white : R.color.currency_subheading_color, null));
        mBalancePrimary.setTextColor(getResources().getColor(cryptoPreferred
                ? R.color.currency_subheading_color : R.color.white, null));
        String circularBoldFont = getString(R.string.Font_CircularPro_Bold);
        String circularBookFont = getString(R.string.Font_CircularPro_Book);
        mBalanceSecondary.setTypeface(FontManager.get(this, cryptoPreferred ? circularBoldFont : circularBookFont));
        mBalancePrimary.setTypeface(FontManager.get(this, cryptoPreferred ? circularBookFont : circularBoldFont));

        updateUi();
    }

    private synchronized void showSendIfNeeded(final Intent intent) {
        final CryptoRequest request = (CryptoRequest) intent.getSerializableExtra(EXTRA_CRYPTO_REQUEST);
        intent.removeExtra(EXTRA_CRYPTO_REQUEST);
        if (request != null) {
            showSendFragment(request);
        }
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
            if (mListGroupLayout.getChildCount() >= MAX_NUMBER_OF_CHILDREN) {
                mListGroupLayout.removeViewAt(0);
            }
            mListGroupLayout.addView(promptView, 0);
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
        RatesDataSource.getInstance(this).addOnDataChangedListener(this);
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (wallet != null) {
            wallet.addTxListModifiedListener(this);
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    WalletEthManager.getInstance(HomeActivity.this).estimateGasPrice();
                    wallet.refreshCachedBalance(HomeActivity.this);
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            updateUi();
                        }
                    });
                    if (wallet.getConnectStatus() != 2) {
                        wallet.connect(HomeActivity.this);
                    }

                }
            });

            wallet.addBalanceChangedListener(this);

            mCurrentWalletIso = wallet.getCurrencyCode();

            if (!TokenUtil.isTokenSupported(mCurrentWalletIso)) {
                showDelistedTokenBanner();
            }
            wallet.addSyncListener(this);
        }

        mSyncNotificationBroadcastReceiver = new SyncNotificationBroadcastReceiver();
        SyncService.registerSyncNotificationBroadcastReceiver(getApplicationContext(),
                mSyncNotificationBroadcastReceiver);
        SyncService.startService(getApplicationContext(), mCurrentWalletIso);

        showSendIfNeeded(getIntent());

    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
        if (mWallet != null) {
            mWallet.removeSyncListener(this);
        }
        SyncService.unregisterSyncNotificationBroadcastReceiver(getApplicationContext(),
                mSyncNotificationBroadcastReceiver);
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

    /**
     * The {@link HomeActivity.SyncNotificationBroadcastReceiver} is responsible for receiving updates from the
     * {@link SyncService} and updating the UI accordingly.
     */
    private class SyncNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncService.ACTION_SYNC_PROGRESS_UPDATE.equals(intent.getAction())) {
                String intentWalletIso = intent.getStringExtra(SyncService.EXTRA_WALLET_CURRENCY_CODE);
                double progress = intent.getDoubleExtra(SyncService.EXTRA_PROGRESS, SyncService.PROGRESS_NOT_DEFINED);
                if (mCurrentWalletIso.equals(intentWalletIso)) {
                    if (progress >= SyncService.PROGRESS_START) {
                        HomeActivity.this.updateSyncProgress(progress);
                    } else {
                        Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Progress not set:" + progress);
                    }
                } else {
                    Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Wrong wallet. Expected:"
                            + mCurrentWalletIso + " Actual:" + intentWalletIso + " Progress:" + progress);
                }
            }
        }
    }

    public void closeNotificationBar() {
        mNotificationBar.setVisibility(View.INVISIBLE);
    }

    public void updateSyncProgress(double progress) {
        if (progress != SyncService.PROGRESS_FINISH) {
            StringBuffer labelText = new StringBuffer(getString(R.string.SyncingView_syncing));
            labelText.append(' ')
                    .append(NumberFormat.getPercentInstance().format(progress));
            mProgressLabel.setText(labelText);
            mProgressLayout.setVisibility(View.VISIBLE);

            if (mWallet instanceof BaseBitcoinWalletManager) {
                BaseBitcoinWalletManager baseBitcoinWalletManager = (BaseBitcoinWalletManager) mWallet;
                long syncThroughDateInMillis = baseBitcoinWalletManager.getPeerManager()
                        .getLastBlockTimestamp() * DateUtils.SECOND_IN_MILLIS;
                String syncedThroughDate = new SimpleDateFormat(SYNCED_THROUGH_DATE_FORMAT,
                        Locale.getDefault()).format(syncThroughDateInMillis);
                mSyncStatusLabel.setText(String.format(getString(R.string.SyncingView_syncedThrough),
                        syncedThroughDate));
            }
        } else {
            mProgressLayout.animate()
                    .translationY(-mProgressLayout.getHeight())
                    .alpha(SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA)
                    .setDuration(DateUtils.SECOND_IN_MILLIS)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mProgressLayout.setVisibility(View.GONE);
                        }
                    });
        }
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