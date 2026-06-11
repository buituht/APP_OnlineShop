package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private OnCategoryActionListener listener;
    // id → name map để hiển thị tên danh mục cha
    private Map<String, String> categoryNameMap;

    public interface OnCategoryActionListener {
        void onEdit(Category category);
        void onDelete(Category category);
        void onToggleShowOnHome(Category category, boolean showOnHome);
    }

    public CategoryAdapter(List<Category> categoryList, OnCategoryActionListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    public void setCategoryNameMap(Map<String, String> map) {
        this.categoryNameMap = map;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_manage, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvName.setText(category.getName());
        holder.tvSort.setText(String.valueOf(category.getSortOrder()));

        Glide.with(holder.itemView.getContext())
                .load(GlideUtils.getGlideUrlWithUserAgent(category.getImageUrl()))
                .placeholder(R.drawable.ic_ball)
                .into(holder.ivImage);

        // Hiển thị badge danh mục cha nếu là danh mục con
        String parentId = category.getParentId();
        if (parentId != null && !parentId.isEmpty()) {
            String parentName = (categoryNameMap != null && categoryNameMap.containsKey(parentId))
                    ? categoryNameMap.get(parentId) : parentId;
            holder.tvParent.setVisibility(View.VISIBLE);
            holder.tvParent.setText("Con của: " + parentName);
        } else {
            holder.tvParent.setVisibility(View.GONE);
        }

        // Switch showOnHome — tắt listener trước khi set để tránh trigger thừa
        holder.switchShowOnHome.setOnCheckedChangeListener(null);
        holder.switchShowOnHome.setChecked(category.isShowOnHome());
        holder.switchShowOnHome.setOnCheckedChangeListener((btn, isChecked) -> {
            category.setShowOnHome(isChecked);
            listener.onToggleShowOnHome(category, isChecked);
        });

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(category));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(category));
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvParent, tvSort;
        SwitchCompat switchShowOnHome;
        ImageButton btnEdit, btnDelete;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_item_cat_image);
            tvName = itemView.findViewById(R.id.tv_item_cat_name);
            tvParent = itemView.findViewById(R.id.tv_item_cat_parent);
            tvSort = itemView.findViewById(R.id.tv_item_cat_sort);
            switchShowOnHome = itemView.findViewById(R.id.switch_show_on_home);
            btnEdit = itemView.findViewById(R.id.btn_item_cat_edit);
            btnDelete = itemView.findViewById(R.id.btn_item_cat_delete);
        }
    }
}
