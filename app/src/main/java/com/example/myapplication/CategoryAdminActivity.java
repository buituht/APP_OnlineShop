package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryAdminActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryActionListener {

    private List<Category> categoryList;
    private CategoryAdapter adapter;
    private RecyclerView rvCategories;
    private CategoryDAO categoryDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_admin);

        categoryDAO = new CategoryDAO(this);
        categoryList = new ArrayList<>();
        rvCategories = findViewById(R.id.rv_categories_admin);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CategoryAdapter(categoryList, this);
        rvCategories.setAdapter(adapter);

        loadCategories();

        findViewById(R.id.btn_back_category).setOnClickListener(v -> finish());
        findViewById(R.id.fab_add_category).setOnClickListener(v ->
                startActivity(new Intent(this, AddCategoryActivity.class)));
    }

    private void loadCategories() {
        categoryDAO.getAllCategories().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryList.clear();
            Map<String, String> nameMap = new HashMap<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Category cat = doc.toObject(Category.class);
                if (cat != null) {
                    cat.setId(doc.getId());
                    categoryList.add(cat);
                    nameMap.put(cat.getId(), cat.getName());
                }
            }
            // Sắp xếp: danh mục cha lên trước, sau đó theo sortOrder
            categoryList.sort((a, b) -> {
                boolean aIsChild = a.getParentId() != null && !a.getParentId().isEmpty();
                boolean bIsChild = b.getParentId() != null && !b.getParentId().isEmpty();
                if (aIsChild != bIsChild) return aIsChild ? 1 : -1;
                return Integer.compare(a.getSortOrder(), b.getSortOrder());
            });
            adapter.setCategoryNameMap(nameMap);
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onEdit(Category category) {
        Intent intent = new Intent(this, AddCategoryActivity.class);
        intent.putExtra("is_edit", true);
        intent.putExtra("category_data", category);
        startActivity(intent);
    }

    @Override
    public void onDelete(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục '" + category.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) ->
                        categoryDAO.deleteCategory(category.getId()).addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        }).addOnFailureListener(e ->
                                Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onToggleShowOnHome(Category category, boolean showOnHome) {
        categoryDAO.updateCategory(category).addOnSuccessListener(aVoid ->
                Toast.makeText(this,
                        category.getName() + (showOnHome ? " hiển thị trang chủ" : " ẩn khỏi trang chủ"),
                        Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }
}
