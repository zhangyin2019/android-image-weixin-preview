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

                    // 底部选项弹窗
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                    String[] strings = new String[]{"分享", "保存相册", "取消"};
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(activity,
                            R.layout.item_custom_bottom_sheet_dialog, strings);
                    ListView listView = new ListView(activity);
                    listView.setAdapter(stringArrayAdapter);
                    listView.setDivider(new ColorDrawable(ContextCompat.getColor(activity, R.color.col_eee)));
                    listView.setDividerHeight(Utils.dp2px(activity, 1));
                    listView.setOnItemClickListener((parent, view, position1, id) -> {
                        String name = ((TextView) view).getText().toString();
                        switch (name) {
                            case "分享":
                                shareImage(imgUrlList.get(position));
                                bottomSheetDialog.dismiss();
                                break;
                            case "保存相册":
                                // 权限
                                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Activity.RESULT_FIRST_USER);
                                } else {
                                    saveImageToAlbum(imgUrlList.get(position));
                                    bottomSheetDialog.dismiss();
                                }
                                break;
                            default:
                                bottomSheetDialog.dismiss();
                        }
                    });
                    bottomSheetDialog.setContentView(listView);
                    bottomSheetDialog.show();

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
     * 分享
     */
    private void shareImage(String path) {
        AlertDialog dialog = Utils.setLoadingDialog(activity, "请稍后..", 4000);
        dialog.show();

        new Thread(() -> {
            try {
                String suffix = Utils.getFileSuffix(path, ".png");
                String filename = System.currentTimeMillis() + suffix;

                String saveDir = activity.getExternalCacheDir().getAbsolutePath();
                String savePath = saveDir + "/" + filename;
                File file = new File(savePath);

                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                conn.connect();

                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                try {
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Uri imageUri = FileProvider.getUriForFile(activity, "com.sanxin.download.fileProvider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                activity.startActivity(Intent.createChooser(intent, "分享到"));
                activity.runOnUiThread(dialog::dismiss);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 保存相册
     */
    private void saveImageToAlbum(String path) {
        String suffix = Utils.getFileSuffix(path, ".png");
        String filename = System.currentTimeMillis() + suffix;

        /*
        SDCard地址 /storage/emulated/0
        getExternalStorageDirectory在29已废弃
        String saveDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        getExternalFilesDir()  用于获取SDCard/Android/data/你的应用的包名/files/ 目录
        */
        File externalFileRootDir = activity.getExternalFilesDir(null);
        do {
            externalFileRootDir = Objects.requireNonNull(externalFileRootDir).getParentFile();
        } while (Objects.requireNonNull(externalFileRootDir).getAbsolutePath().contains("/Android"));

        String saveDir = Objects.requireNonNull(externalFileRootDir).getAbsolutePath();
        String savePath = saveDir + "/" + Environment.DIRECTORY_DCIM + "/" + filename;// 文件保存地址

        // 29已废弃
        /*Uri imageUri = Uri.parse(MediaStore.Images.Media.insertImage(CustomPreviewActivity.this.getContentResolver(), bitmap, null, null));*/

        Toast downLoadToast = Toast.makeText(activity, "正在下载", Toast.LENGTH_LONG);
        downLoadToast.show();

        new Thread(() -> {
            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                conn.connect();
                int fileLength = conn.getContentLength();// 文件总大小

                // 生成一个大小相同的本地文件
                RandomAccessFile file = new RandomAccessFile(savePath, "rwd");
                file.setLength(fileLength);
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024 * 10 * 2];// 每最多20KB存一次
                int len, stepLen = 0;

                // 从输入流中读取数据,读到缓冲区中
                while ((len = is.read(buffer)) > 0) {
                    stepLen += len;
                    double rate = (double) stepLen / (double) fileLength * 100;
                    activity.runOnUiThread(() -> downLoadToast.setText(String.format(Locale.CHINESE, "已下载： %.2f%%", rate)));
                    file.write(buffer, 0, len);
                }
                activity.runOnUiThread(() -> {
                    downLoadToast.cancel();
                    Toast.makeText(activity, "已存至相册", Toast.LENGTH_SHORT).show();
                });

                // 广播给相册，否则相册将不会更新
                // 29已废弃
                /*Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(new File(savePath)));
                sendBroadcast(intent);*/
                File file1 = new File(savePath);
                MediaScannerConnection.scanFile(activity,
                        new String[]{file1.toString()}, new String[]{file1.getName()}, null);

                // 关闭输入输出流
                is.close();
                file.close();
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
