package com.jinvovo.jinvovoparty.config;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.ScreenUtils;
import com.jinvovo.jinvovoparty.R;
import com.jinvovo.jinvovoparty.adapter.CustomPreviewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CustomPreview {
    Activity activity;
    View view;
    Integer currentPosition = 0;// 当前预览图片序号
    List<ImageView> imageViewList;
    Map<Integer, Bitmap> bitmapMap = new HashMap<>();
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

    public CustomPreview(Activity activity, List<String> imgUrlList, List<ImageView> imageViewList) {
        this.activity = activity;
        this.imgUrlList = imgUrlList;
        this.imageViewList = imageViewList;
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
        window_w = ScreenUtils.getScreenWidth();
        window_h = ScreenUtils.getScreenHeight();

        // 部署dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.full_screen);
        view = View.inflate(activity, R.layout.activity_custom_preview, null);
        builder.setView(view);
        dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(R.color.col_000000_0_per);// 内容窗透明
        dialog.setCancelable(false);
        dialog.setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (i == KeyEvent.KEYCODE_BACK) {
                exit();
                return true;
            }
            return false;
        });

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
        customPreviewAdapter = new CustomPreviewAdapter(imgUrlList)
                .setOnViewTapListener(view -> exit())
                .setOnLongClickListener(v -> {
                    // 自定义长按操作

                    return false;
                })
                .setImageInfoCallback((position, resource) -> {
                    bitmapMap.put(position, resource);
                    if (!isStartEffect) startEffect();
                });
        preview_view_page.setAdapter(customPreviewAdapter);

        // 标题
        preview_description = view.findViewById(R.id.preview_description);
    }

    /**
     * 退出
     */
    @SuppressWarnings("ConstantConditions")
    private void exit() {
        if (locationMapList != null) {
            // 恢复缩放
            customPreviewAdapter.resetScale();

            Map<String, Integer> o = locationMapList.get(currentPosition);
            int left = o.get("left");
            int top = o.get("top");
            int width = o.get("width");
            int height = o.get("height");

            Bitmap map = bitmapMap.get(currentPosition);
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
    @SuppressWarnings("ConstantConditions")
    private void startEffect() {
        isStartEffect = true;

        if (imgUrlList == null && imgUrlList == null) return;

        if (titleList == null) {
            preview_description.setVisibility(View.GONE);
        } else {
            preview_description.setText(titleList.get(currentPosition));
        }

        // 仿微信效果
        if (imageViewList != null) {
            locationMapList = new ArrayList<>();
            int[] loc = new int[2];
            for (ImageView view1 : imageViewList) {
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

            Bitmap map = bitmapMap.get(currentPosition);
            int ori_width = map.getWidth();
            int ori_height = map.getHeight();

            // 初始
            preview_layout.animate().alpha(1f).setDuration(1).start();// 直接setAlpha会闪一下
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
