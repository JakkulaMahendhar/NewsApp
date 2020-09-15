package com.appyhigh.newsapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.appyhigh.newsapp.AndroidUtils.Utils;
import com.appyhigh.newsapp.R;
import com.appyhigh.newsapp.databinding.ListItemNewsBinding;
import com.appyhigh.newsapp.model.Article;
import com.appyhigh.newsapp.view.MainActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.List;


public class NewsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MENU_ITEM_VIEWTYPE = 0;
    private static final int NATIVE_ADTYPE = 1;
    private List<Article> articles;
    private Context context;
    private OnNewsClickListener onNewsClickListener;
    ListItemNewsBinding listItemNewsBinding;

    public NewsListAdapter(List<Article> articles, Context context, OnNewsClickListener onNewsClickListener) {
        this.articles = articles;
        this.context = context;
        this.onNewsClickListener = onNewsClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case NATIVE_ADTYPE:
                View unifiedNativeLayoutView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.ad_unified,
                        parent, false);
                return new UnifiedNativeAdViewHolder(unifiedNativeLayoutView);
            case MENU_ITEM_VIEWTYPE:
            default:
                listItemNewsBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.list_item_news, parent, false);
                return new ViewHolder(listItemNewsBinding, onNewsClickListener);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            int viewType = getItemViewType(position);
            Article article = new Article();
            switch (viewType) {
                case MENU_ITEM_VIEWTYPE:
                    ViewHolder viewHolder1 = (ViewHolder) holder;
                    article = articles.get(position);
                    if (article != null) {
                        viewHolder1.listItemNewsBinding.setArticle(article);
                    }
                case NATIVE_ADTYPE:
                    UnifiedNativeAdViewHolder unifiedNativeAdViewHolder = (UnifiedNativeAdViewHolder) holder;
                    UnifiedNativeAd nativeAd = (UnifiedNativeAd) articles.get(position).getNativeAd();
                    populateNativeAdView(nativeAd, unifiedNativeAdViewHolder.getAdView());
                    break;
                default:
                    ViewHolder viewHolder = (ViewHolder) holder;
                    article = articles.get(position);
                    if (article != null) {
                        viewHolder.listItemNewsBinding.setArticle(article);
                    }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public interface OnNewsClickListener {
        void onNewsClick(int position);

        void onSaveClick(int position);
    }

    @BindingAdapter("newsimage")
    public static void setImage(ImageView image, String Url) {
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(new ColorDrawable(Color.parseColor("#f9f9fa")))
                .error(new ColorDrawable(Color.parseColor("#f9f9fa")))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

        Glide.with(image.getContext())
                .load(Url)
                .apply(requestOptions)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(image);

    }

    @BindingAdapter("newsdate")
    public static void setDate(TextView textView, String date) {
        textView.setText(Utils.dateFormat(date));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {


        ListItemNewsBinding listItemNewsBinding;

        public ViewHolder(ListItemNewsBinding itemView, final OnNewsClickListener onNewsClickListener) {
            super(itemView.getRoot());
            this.listItemNewsBinding = itemView;
            itemView.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onNewsClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        onNewsClickListener.onNewsClick(getAdapterPosition());
                    }
                }
            });
        }


    }


    @Override
    public int getItemViewType(int position) {

        Article recyclerViewItem = articles.get(position);
        if (recyclerViewItem.getNativeAd() != null) {
            return NATIVE_ADTYPE;
        }
        return MENU_ITEM_VIEWTYPE;
    }


    private void populateNativeAdView(UnifiedNativeAd nativeAd,
                                      UnifiedNativeAdView adView) {
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

        NativeAd.Image icon = nativeAd.getIcon();

        if (icon == null) {
            adView.getIconView().setVisibility(View.INVISIBLE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // Assign native ad object to the native view.
        adView.setNativeAd(nativeAd);
    }
}
