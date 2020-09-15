package com.appyhigh.newsapp.di;


import com.appyhigh.newsapp.view.MainActivity;
import com.appyhigh.newsapp.view.NewsDetailsActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ActivityBuildersModule {

    @ContributesAndroidInjector(
            modules = {NewsViewModelsModule.class}
    )
    abstract MainActivity contributeNewsActivity();

    @ContributesAndroidInjector
    abstract NewsDetailsActivity contributeNewsDetailsActivity();
}