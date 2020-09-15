package com.appyhigh.newsapp.view;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appyhigh.newsapp.AndroidUtils.Constants;
import com.appyhigh.newsapp.AndroidUtils.NetworkUtil;
import com.appyhigh.newsapp.BuildConfig;
import com.appyhigh.newsapp.GpsTracker;
import com.appyhigh.newsapp.R;
import com.appyhigh.newsapp.adapter.NewsListAdapter;
import com.appyhigh.newsapp.databinding.ActivityMainBinding;
import com.appyhigh.newsapp.model.Article;
import com.appyhigh.newsapp.model.News;
import com.appyhigh.newsapp.viewmodel.NewsResource;
import com.appyhigh.newsapp.viewmodel.NewsViewModel;
import com.appyhigh.newsapp.viewmodel.ViewModelProviderFactory;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

public class MainActivity extends DaggerAppCompatActivity implements NewsListAdapter.OnNewsClickListener {

    @Inject
    ViewModelProviderFactory providerFactory;
    private NewsViewModel viewModel;
    private ProgressBar progressBar;

    @Inject
    NetworkUtil networkUtil;

    private RecyclerView recyclerView;
    private List<Article> articles = new ArrayList<>();
    private NewsListAdapter adapter;
    private TextView newsError;
    private GpsTracker gpsTracker;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    ActivityMainBinding activityMainBinding;
    AdView mAdView;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    public static final int ITEMS_PER_AD = 5;
    private static final int NATIVE_AD_HEIGHT = 150;
    private static final String NATIVE_AD_UNITID = "ca-app-pub-1468710847656510/9468388845";

    // The number of native ads to load.
    public static final int NUMBER_OF_ADS = 5;

    // The AdLoader used to load ads.
    private AdLoader adLoader;

    // List of native ads that have been successfully loaded.
    private List<UnifiedNativeAd> mNativeAds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        progressBar = activityMainBinding.progressBar;
        newsError = activityMainBinding.newsError;
        checkPermissions();
        //initViewModel();
        //getNewsList();
        //setupRecyclerView();
        MobileAds.initialize(this, "ca-app-pub-1468710847656510~4373341819");
        //loadNativeAds();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("TAG", "Config params updated: " + updated);
                            Toast.makeText(MainActivity.this, "Fetch and activate succeeded",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(MainActivity.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        displayAds();
                    }
                });
    }

    private void displayAds() {
        boolean isAds = mFirebaseRemoteConfig.getBoolean("show_ads");
        if (!isAds) {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });

            mAdView = activityMainBinding.adView;
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            loadNativeAds();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void checkPermissions() {
        int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        } else {
            getLocation();
        }

    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(MainActivity.this, providerFactory).get(NewsViewModel.class);
        viewModel.init();
    }

    private void setupRecyclerView() {
        recyclerView = activityMainBinding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
            getLocation();
            if (!Constants.COUNTRY_CODE.equals("")) {
                initViewModel();
                getNewsList();
                setupRecyclerView();
            }
        }
    }

    private void getNewsList() {
        showProgressBar(true);
        viewModel.getNewsList()
                .observe(this, new Observer<NewsResource<News>>() {
                    @Override
                    public void onChanged(NewsResource<News> newsNewsResource) {
                        switch (newsNewsResource.status) {
                            case SUCCESS:
                                showProgressBar(false);
                                if (newsNewsResource.data != null && newsNewsResource.data.getStatus().equals(Constants.STATUS_OK)) {
                                    newsError.setVisibility(View.GONE);
                                    if (!articles.isEmpty()) {
                                        articles.clear();
                                    }
                                    articles = newsNewsResource.data.getArticle();
                                    if (articles.size() != 0) {
                                        adapter = new NewsListAdapter(articles, MainActivity.this, MainActivity.this);
//                                        recyclerView.setAdapter(adapter);
//                                        adapter.notifyDataSetChanged();
                                    } else newsError.setVisibility(View.VISIBLE);
                                } else {
                                    newsError.setVisibility(View.VISIBLE);
                                    Toast.makeText(MainActivity.this, "Failed to fetch", Toast.LENGTH_LONG).show();
                                }
                                break;
                            case ERROR:
                                showProgressBar(false);
                                newsError.setVisibility(View.VISIBLE);
                                Toast.makeText(MainActivity.this, "Failed to fetch", Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                });
    }

    private void showProgressBar(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void loadNativeAds() {

        AdLoader.Builder builder = new AdLoader.Builder(this, "ca-app-pub-1468710847656510/9468388845");
        adLoader = builder.forUnifiedNativeAd(
                new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        // A native ad loaded successfully, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        mNativeAds.add(unifiedNativeAd);
                        if (!adLoader.isLoading()) {
                            insertAdsInMenuItems();
                        }
                    }
                }).withAdListener(
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // A native ad failed to load, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        Log.e("MainActivity", "The previous native ad failed to load. Attempting to"
                                + " load another.");
                        if (!adLoader.isLoading()) {
                            insertAdsInMenuItems();
                        }
                    }
                }).build();

        // Load the Native ads.
        adLoader.loadAds(new AdRequest.Builder().build(), NUMBER_OF_ADS);
    }

    private void insertAdsInMenuItems() {
        if (mNativeAds.size() <= 0) {
            return;
        }

        int offset = (articles.size() / mNativeAds.size()) + 1;
        int index = 0;
        for (UnifiedNativeAd ad : mNativeAds) {
            Article article = new Article();
            article.setNativeAd(ad);
            articles.add(index, article);
            index = index + offset;
        }
        if (adapter != null) {
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onNewsClick(int position) {
        Intent newsDetailsIntent = NewsDetailsActivity.newIntent(this, articles.get(position).getSource().getName(), articles.get(position).getUrl());
        startActivity(newsDetailsIntent);
    }

    @Override
    public void onSaveClick(int position) {

    }


    public void getLocation() {
        gpsTracker = new GpsTracker(MainActivity.this);
        if (gpsTracker.canGetLocation()) {
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            getAddress(latitude, longitude);
        } else {
            gpsTracker.showSettingsAlert();
        }
    }

    public void getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj;
            // =new Address(Locale.getDefault())
            String add;
            if (!addresses.isEmpty()) {
                obj = addresses.get(0);
                Constants.COUNTRY_CODE = obj.getCountryCode();

            }
            // Log.v("IGA", "Address" + add);
            //Toast.makeText(this, "Address=>" + add, Toast.LENGTH_SHORT).show();

            // TennisAppActivity.showDialog(add);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
