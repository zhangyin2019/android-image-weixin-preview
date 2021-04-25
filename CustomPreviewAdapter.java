package com.sanxin.adapter;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.sanxin.R;

import java.util.ArrayList;
import java.util.List;

import me.relex.photodraweeview.OnViewTapListener;
import me.relex.photodraweeview.PhotoDraweeView;

public class CustomPreviewAdapter extends RecyclerView.Adapter<CustomPreviewAdapter.CustomPreviewHolder> {
    List<String> imgUrlList;
    List<SimpleDraweeView> simpleDraweeViewList;
    static List<PhotoDraweeView> photoDraweeViewList = new ArrayList<>();

    OnViewTapListener onViewTapListener;
    View.OnLongClickListener onLongClickListener;

    public interface Callback {
        void onSuccess(int position, ImageInfo imageInfo);
    }

    Callback imageInfoCallback;

    public CustomPreviewAdapter(List<String> imgUrlList, List<SimpleDraweeView> simpleDraweeViewList) {
        this.imgUrlList = imgUrlList;
        this.simpleDraweeViewList = simpleDraweeViewList;
    }

    public CustomPreviewAdapter setImageInfoCallback(Callback imageInfoCallback) {
        this.imageInfoCallback = imageInfoCallback;
        return this;
    }

    /**
     * 重置缩放大小
     */
    public void resetScale() {
        for (PhotoDraweeView item : photoDraweeViewList) {
            item.setScale(1.0f, true);
        }
    }

    /**
     * 设置图片点击事件
     *
     * @param onViewTapListener -
     * @return -
     */
    public CustomPreviewAdapter setOnViewTapListener(OnViewTapListener onViewTapListener) {
        this.onViewTapListener = onViewTapListener;
        return this;
    }

    /**
     * 设置图片长按事件
     *
     * @param onLongClickListener -
     * @return -
     */
    public CustomPreviewAdapter setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
        return this;
    }

    @NonNull
    @Override
    public CustomPreviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_custom_preview, parent, false);
        return new CustomPreviewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomPreviewHolder holder, int position) {
        holder.thumb.setTag(position);
        holder.thumb.setController(Fresco.newDraweeControllerBuilder()
                .setUri(imgUrlList.get(position))
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        holder.thumb.setPhotoUri(Uri.parse(imgUrlList.get(position)));
                        imageInfoCallback.onSuccess(position, imageInfo);
                    }
                }).build());
        holder.thumb.setOnViewTapListener(onViewTapListener);
        holder.thumb.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public int getItemCount() {
        return imgUrlList != null ? imgUrlList.size() : 0;
    }

    static class CustomPreviewHolder extends RecyclerView.ViewHolder {
        PhotoDraweeView thumb;

        public CustomPreviewHolder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);

            CustomPreviewAdapter.photoDraweeViewList.add(thumb);
        }
    }
}
