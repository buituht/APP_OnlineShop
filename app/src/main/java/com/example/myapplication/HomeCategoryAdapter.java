package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class HomeCategoryAdapter extends RecyclerView.Adapter<HomeCategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public HomeCategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvName.setText(category.getName());
        
        boolean isSelected = position == selectedPosition;
        String imageUrl = category.getImageUrl();
        boolean hasImage = imageUrl != null && !imageUrl.isEmpty();
        android.content.Context ctx = holder.itemView.getContext();
        int purple = ContextCompat.getColor(ctx, R.color.purple_main);

        // Nền và text theo trạng thái selected
        if (isSelected) {
            holder.cardCategory.setCardBackgroundColor(purple);
            holder.tvName.setTextColor(purple);
            holder.tvName.setTypeface(null, Typeface.BOLD);
        } else {
            holder.cardCategory.setCardBackgroundColor(Color.parseColor("#F3F0FF"));
            holder.tvName.setTextColor(Color.parseColor("#555555"));
            holder.tvName.setTypeface(null, Typeface.NORMAL);
        }

        // Hình ảnh
        if (hasImage) {
            holder.ivImage.setPadding(0, 0, 0, 0);
            holder.ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.ivImage.clearColorFilter();
            Glide.with(ctx)
                    .load(GlideUtils.getGlideUrlWithUserAgent(imageUrl))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .placeholder(R.drawable.ic_ball)
                    .error(R.drawable.ic_ball)
                    .into(holder.ivImage);
        } else {
            int pad = (int)(10 * ctx.getResources().getDisplayMetrics().density);
            holder.ivImage.setPadding(pad, pad, pad, pad);
            holder.ivImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.ivImage.setImageResource(category.getId().equals("all")
                    ? android.R.drawable.ic_menu_sort_by_size
                    : android.R.drawable.ic_menu_gallery);
            holder.ivImage.setColorFilter(isSelected ? Color.WHITE : Color.parseColor("#7E57C2"));
        }

        // 3. Sự kiện nhấn
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCategory;
        ImageView ivImage;
        TextView tvName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // SỬA LỖI: Tìm ID chính xác từ XML
            cardCategory = itemView.findViewById(R.id.card_category);
            ivImage = itemView.findViewById(R.id.iv_category_icon);
            tvName = itemView.findViewById(R.id.tv_category_name);
        }
    }
}
