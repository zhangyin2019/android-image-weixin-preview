package com.jinvovo.jinvovoparty.adapter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.jinvovo.jinvovoparty.R;

import java.util.ArrayList;
import java.util.List;

public class CustomPreviewAdapter extends RecyclerView.Adapter<CustomPreviewAdapter.CustomPreviewHolder> {
    List<String> imgUrlList;
    static List<PhotoView> photoViewList = new ArrayList<>();

    View.OnClickListener onViewTapListener;
    View.OnLongClickListener onLongClickListener;

    public interface Callback {
        void onSuccess(int position, Bitmap resource);
    }

    Callback imageInfoCallback;

    public CustomPreviewAdapter(List<String> imgUrlList) {
        this.imgUrlList = imgUrlList;
    }

    public CustomPreviewAdapter setImageInfoCallback(Callback imageInfoCallback) {
        this.imageInfoCallback = imageInfoCallback;
        return this;
    }

    /**
     * 重置缩放大小
     */
    public void resetScale() {
        for (PhotoView item : photoViewList) {
            item.setScale(1.0f, true);
        }
    }

    /**
     * 设置图片点击事件
     *
     * @param onViewTapListener -
     * @return -
     */
    public CustomPreviewAdapter setOnViewTapListener(View.OnClickListener onViewTapListener) {
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
        Glide.with(parent).asGif()
                .load("http://images.ylwx365.com/images/mini/73371620977260966.gif")
                .into(((ImageView) view.findViewById(R.id.loading)));
        return new CustomPreviewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomPreviewHolder holder, int position) {
        holder.thumb.setTag(position);
        Glide.with(holder.thumb).asBitmap().load(imgUrlList.get(position))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        holder.thumb.setImageBitmap(resource);
                        imageInfoCallback.onSuccess(position, resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
        holder.thumb.setOnClickListener(onViewTapListener);
        holder.thumb.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public int getItemCount() {
        return imgUrlList != null ? imgUrlList.size() : 0;
    }

    static class CustomPreviewHolder extends RecyclerView.ViewHolder {
        PhotoView thumb;

        public CustomPreviewHolder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);

            CustomPreviewAdapter.photoViewList.add(thumb);
        }
    }
}
