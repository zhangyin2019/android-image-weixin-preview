package com.sanxin.config;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sanxin.R;
import com.sanxin.adapter.CustomPreviewAdapter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class CustomPreview {
    Activity activity;
    View view;
    Integer currentPosition = 0;// 当前预览图片序号
    List<SimpleDraweeView> simpleDraweeViewList;
    Map<Integer, ImageInfo> imageInfoMap = new HashMap<>();
    List<Map<String, Integer>> locationMapList;
    List<String> imgUrlList;
    List<String> titleList;

    Integer window_w;
    Integer window_h;

    Boolean isStartEffect = false;
    int durationTime = 300;

    AlertDialog dialog;
    RelativeLayout preview_layout;
    ViewPager2 preview_view_page;
    CustomPreviewAdapter customPreviewAdapter;
    RelativeLayout preview_view_page_layout;
    RelativeLayout.LayoutParams preview_view_page_layout_lp;
    View preview_bg;
    TextView preview_description;

    public CustomPreview(Activity activity, List<String> imgUrlList) {
        this(activity, imgUrlList, null);
    }

    public CustomPreview(Activity activity, List<String> imgUrlList, List<SimpleDraweeView> simpleDraweeViewList) {
        this.activity = activity;
        this.imgUrlList = imgUrlList;
        this.simpleDraweeViewList = simpleDraweeViewList;
        init();
    }

    public CustomPreview setTitleList(List<String> titleList) {
        this.titleList = titleList;
        return this;
    }

    public CustomPreview setCurrentPosition(Integer currentPosition) {
        this.currentPosition = currentPosition;
        preview_view_page.setCurrentItem(currentPosition, false);
        return this;
    }

    /**
     * 部署
     */
    private void init() {
        window_w = Utils.getDeviceInfo(activity).get("width");
        window_h = Utils.getDeviceInfo(activity).get("height");

        // 部署dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.full_screen);
        view = View.inflate(activity, R.layout.activity_custom_preview, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(R.color.col_000000_0_per);// 内容窗透明

        // 页面默认先透明
        preview_layout = view.findViewById(R.id.preview_layout);
        preview_layout.setAlpha(0);

        // viewpager部署
        preview_view_page_layout = view.findViewById(R.id.preview_view_page_layout);
        preview_view_page = view.findViewById(R.id.preview_view_page);
        preview_view_page.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                if (titleList != null) preview_description.setText(titleList.get(currentPosition));
            }
        });
        customPreviewAdapter = new CustomPreviewAdapter(imgUrlList, simpleDraweeViewList)
                .setOnViewTapListener((view, x, y) -> exit())
                .setOnLongClickListener(v -> {
                    int position = (int) v.getTag();

                    // TODO by yourself

                    return false;
                })
                .setImageInfoCallback((position, imageInfo) -> {
                    imageInfoMap.put(position, imageInfo);
                    if (!isStartEffect) startEffect();
                });
        preview_view_page.setAdapter(customPreviewAdapter);

        // 标题
        preview_description = view.findViewById(R.id.preview_description);
    }

    /**
     * 退出
     */
    private void exit() {
        if (locationMapList != null) {
            // 恢复缩放
            customPreviewAdapter.resetScale();

            Map<String, Integer> o = locationMapList.get(currentPosition);
            int left = o.get("left");
            int top = o.get("top");
            int width = o.get("width");
            int height = o.get("height");

            ImageInfo map = imageInfoMap.get(currentPosition);
            int ori_width = map.getWidth();
            int ori_height = map.getHeight();

            // 恢复centerCrop的效果
            float ratio = (float) width / height, ori_ratio = (float) ori_width / ori_height, diffRatio;
            int diff;
            if (ratio >= ori_ratio) {
                diff = width * ori_height / ori_width;
                diffRatio = 1 + ((float) (diff - height) / height);
            } else {
                diff = height * ori_width / ori_height;
                diffRatio = 1 + ((float) (diff - width) / width);
            }

            // 图片过渡
            ValueAnimator valueAnimator = ValueAnimator.ofInt(window_w, width);
            valueAnimator.addUpdateListener(animation -> {
                preview_view_page_layout_lp.width = (int) animation.getAnimatedValue();
                preview_view_page_layout.setLayoutParams(preview_view_page_layout_lp);
            });
            valueAnimator.setDuration(durationTime).start();
            valueAnimator = ValueAnimator.ofInt(window_h, height);
            valueAnimator.addUpdateListener(animation -> {
                preview_view_page_layout_lp.height = (int) animation.getAnimatedValue();
                preview_view_page_layout.setLayoutParams(preview_view_page_layout_lp);
            });
            valueAnimator.setDuration(durationTime).start();
            preview_view_page_layout.animate().translationX(left).translationY(top).setDuration(durationTime).start();
            preview_view_page.animate().scaleX(diffRatio).scaleY(diffRatio).setDuration(durationTime).start();

            // 文字过渡
            preview_description.animate().alpha(0).setDuration(durationTime).start();

            // 背景过渡
            preview_bg.animate().alpha(0).setDuration(durationTime).start();
        } else {
            preview_layout.animate().alpha(0).setDuration(durationTime).start();
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, durationTime);

    }

    /**
     * 启动效果
     */
    private void startEffect() {
        isStartEffect = true;

        if (imgUrlList == null && simpleDraweeViewList == null) return;

        if (titleList == null) {
            preview_description.setVisibility(View.GONE);
        } else {
            preview_description.setText(titleList.get(currentPosition));
        }

        // 仿微信效果
        if (simpleDraweeViewList != null) {
            locationMapList = new ArrayList<>();
            int[] loc = new int[2];
            for (SimpleDraweeView view1 : simpleDraweeViewList) {
                view1.getLocationInWindow(loc);
                Map<String, Integer> map = new HashMap<>();
                map.put("left", loc[0]);
                map.put("top", loc[1]);
                map.put("width", view1.getWidth());
                map.put("height", view1.getHeight());
                locationMapList.add(map);
            }

            Map<String, Integer> o = locationMapList.get(currentPosition);
            int left = o.get("left");
            int top = o.get("top");
            int width = o.get("width");
            int height = o.get("height");

            ImageInfo map = imageInfoMap.get(currentPosition);
            int ori_width = map.getWidth();
            int ori_height = map.getHeight();

            // 初始
            preview_layout.setAlpha(1);
            preview_view_page_layout.setTranslationX(left);
            preview_view_page_layout.setTranslationY(top);
            preview_view_page_layout_lp = (RelativeLayout.LayoutParams) preview_view_page_layout.getLayoutParams();
            preview_view_page_layout_lp.width = width;
            preview_view_page_layout_lp.height = height;
            preview_view_page_layout.setLayoutParams(preview_view_page_layout_lp);

            // 初始centerCrop的效果
            float ratio = (float) width / height, ori_ratio = (float) ori_width / ori_height, diffRatio;
            int diff;
            if (ratio >= ori_ratio) {
                diff = width * ori_height / ori_width;
                diffRatio = 1 + ((float) (diff - height) / height);
            } else {
                diff = height * ori_width / ori_height;
                diffRatio = 1 + ((float) (diff - width) / width);
            }
            preview_view_page.setScaleY(diffRatio);
            preview_view_page.setScaleX(diffRatio);

            // 图片过渡
            ValueAnimator valueAnimator = ValueAnimator.ofInt(width, window_w);
            valueAnimator.addUpdateListener(animation -> {
                preview_view_page_layout_lp.width = (int) animation.getAnimatedValue();
                preview_view_page_layout.setLayoutParams(preview_view_page_layout_lp);
            });
            valueAnimator.setDuration(durationTime).start();
            valueAnimator = ValueAnimator.ofInt(height, window_h);
            valueAnimator.addUpdateListener(animation -> {
                preview_view_page_layout_lp.height = (int) animation.getAnimatedValue();
                preview_view_page_layout.setLayoutParams(preview_view_page_layout_lp);
            });
            valueAnimator.setDuration(durationTime).start();
            preview_view_page_layout.animate().translationX(0).translationY(0).setDuration(durationTime).start();
            preview_view_page.animate().scaleX(1).scaleY(1).setDuration(durationTime).start();

            // 文字过渡
            preview_description.setAlpha(0);
            preview_description.animate().alpha(1).setDuration(durationTime).start();

            // 背景
            preview_bg = view.findViewById(R.id.preview_bg);
            preview_bg.setAlpha(0);
            preview_bg.animate().alpha(1).setDuration(durationTime).start();
        } else {
            preview_layout.animate().alpha(1).setDuration(durationTime).start();
        }
    }

    /**
     * 展示
     */
    public void show() {
        dialog.show();
    }

}
