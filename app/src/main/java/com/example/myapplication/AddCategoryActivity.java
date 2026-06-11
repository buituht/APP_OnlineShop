package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddCategoryActivity extends AppCompatActivity {

    private TextInputEditText etCategoryName, etCategoryImage, etSortOrder;
    private MaterialButton btnSave, btnCancel;
    private View btnSelectImage;
    private ImageView ivPreview;
    private TextView tvTitle;
    private Spinner spinnerParent;
    private SwitchCompat switchShowOnHome;
    private RecyclerView rvCategories;
    private BottomNavigationView bottomNavigationView;

    private CategoryDAO categoryDAO;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList;

    // Dữ liệu cho spinner danh mục cha
    private List<Category> parentOptions = new ArrayList<>(); // chỉ gồm danh mục gốc
    private List<String> parentLabels = new ArrayList<>();
    private String selectedParentId = null;

    private boolean isEditMode = false;
    private Category editingCategory = null;
    private Uri selectedImageUri;
    private boolean isUploading = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivPreview.setImageURI(selectedImageUri);
                    etCategoryImage.setText("");
                    uploadCategoryImage(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        categoryDAO = new CategoryDAO(this);
        initViews();
        setupRecyclerView();
        setupImagePreview();
        setupBottomNavigation();
        loadCategories();

        // Nhận dữ liệu nếu là edit mode
        if (getIntent().getBooleanExtra("is_edit", false)) {
            Category cat = (Category) getIntent().getSerializableExtra("category_data");
            if (cat != null) enterEditMode(cat);
        }

        btnCancel.setOnClickListener(v -> {
            if (isEditMode) resetForm();
            else finish();
        });
        btnSave.setOnClickListener(v -> saveCategory());
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    private void initViews() {
        etCategoryName = findViewById(R.id.et_category_name);
        etCategoryImage = findViewById(R.id.et_category_image);
        etSortOrder = findViewById(R.id.et_category_sort_order);
        btnSave = findViewById(R.id.btn_save_category);
        btnCancel = findViewById(R.id.btn_cancel_category);
        btnSelectImage = findViewById(R.id.btn_select_category_image);
        ivPreview = findViewById(R.id.iv_category_preview);
        tvTitle = findViewById(R.id.tv_add_category_title);
        spinnerParent = findViewById(R.id.spinner_parent_category);
        switchShowOnHome = findViewById(R.id.switch_category_show_on_home);
        rvCategories = findViewById(R.id.rv_categories_manage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            if (id == R.id.nav_products) {
                startActivity(new Intent(this, ProductListActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onEdit(Category category) { enterEditMode(category); }

            @Override
            public void onDelete(Category category) { showDeleteConfirmation(category); }

            @Override
            public void onToggleShowOnHome(Category category, boolean showOnHome) {
                categoryDAO.updateCategory(category).addOnFailureListener(e ->
                        Toast.makeText(AddCategoryActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupImagePreview() {
        etCategoryImage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (!url.isEmpty()) {
                    selectedImageUri = null;
                    Glide.with(AddCategoryActivity.this)
                            .load(GlideUtils.getGlideUrlWithUserAgent(url))
                            .placeholder(R.drawable.ic_ball)
                            .error(R.drawable.ic_ball)
                            .into(ivPreview);
                } else if (selectedImageUri == null) {
                    ivPreview.setImageResource(R.drawable.ic_ball);
                }
            }
        });
    }

    private void loadCategories() {
        categoryDAO.getAllCategories().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryList.clear();
            parentOptions.clear();
            parentLabels.clear();
            Map<String, String> nameMap = new HashMap<>();

            List<Category> allCats = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Category cat = doc.toObject(Category.class);
                if (cat != null) {
                    cat.setId(doc.getId());
                    allCats.add(cat);
                    nameMap.put(cat.getId(), cat.getName());
                }
            }

            // Spinner: chỉ cho phép chọn danh mục gốc làm cha (tránh nested quá sâu)
            parentLabels.add("Không có (danh mục gốc)");
            parentOptions.add(null);
            for (Category cat : allCats) {
                if (cat.getParentId() == null || cat.getParentId().isEmpty()) {
                    // Bỏ qua chính danh mục đang edit
                    if (isEditMode && editingCategory != null && cat.getId().equals(editingCategory.getId())) continue;
                    parentOptions.add(cat);
                    parentLabels.add(cat.getName());
                }
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, parentLabels);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerParent.setAdapter(spinnerAdapter);

            // Khôi phục lựa chọn cha nếu đang edit
            if (isEditMode && editingCategory != null && editingCategory.getParentId() != null) {
                for (int i = 0; i < parentOptions.size(); i++) {
                    Category opt = parentOptions.get(i);
                    if (opt != null && opt.getId().equals(editingCategory.getParentId())) {
                        spinnerParent.setSelection(i);
                        break;
                    }
                }
            }

            spinnerParent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Category chosen = parentOptions.get(position);
                    selectedParentId = (chosen != null) ? chosen.getId() : null;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) { selectedParentId = null; }
            });

            // Cập nhật danh sách hiển thị phía dưới (sắp xếp cha trước, theo sortOrder)
            allCats.sort((a, b) -> {
                boolean aChild = a.getParentId() != null && !a.getParentId().isEmpty();
                boolean bChild = b.getParentId() != null && !b.getParentId().isEmpty();
                if (aChild != bChild) return aChild ? 1 : -1;
                return Integer.compare(a.getSortOrder(), b.getSortOrder());
            });
            categoryList.addAll(allCats);
            categoryAdapter.setCategoryNameMap(nameMap);
            categoryAdapter.notifyDataSetChanged();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void enterEditMode(Category category) {
        isEditMode = true;
        editingCategory = category;
        tvTitle.setText("CẬP NHẬT DANH MỤC");
        btnSave.setText("CẬP NHẬT");
        btnCancel.setText("HỦY");

        etCategoryName.setText(category.getName());
        etCategoryImage.setText(category.getImageUrl());
        etSortOrder.setText(String.valueOf(category.getSortOrder()));
        switchShowOnHome.setChecked(category.isShowOnHome());
        selectedParentId = category.getParentId();
        selectedImageUri = null;

        if (!TextUtils.isEmpty(category.getImageUrl())) {
            Glide.with(this)
                    .load(GlideUtils.getGlideUrlWithUserAgent(category.getImageUrl()))
                    .placeholder(R.drawable.ic_ball)
                    .into(ivPreview);
        }
        // Reload spinner với dữ liệu mới (để loại bỏ chính nó khỏi danh sách cha)
        loadCategories();
    }

    private void resetForm() {
        isEditMode = false;
        editingCategory = null;
        selectedParentId = null;
        selectedImageUri = null;
        tvTitle.setText("THÊM DANH MỤC MỚI");
        btnSave.setText("LƯU");
        btnCancel.setText("HỦY BỎ");
        etCategoryName.setText("");
        etCategoryImage.setText("");
        etSortOrder.setText("0");
        switchShowOnHome.setChecked(true);
        ivPreview.setImageResource(R.drawable.ic_ball);
        spinnerParent.setSelection(0);
        loadCategories();
    }

    private void uploadCategoryImage(Uri uri) {
        isUploading = true;
        btnSave.setEnabled(false);
        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("categories/" + UUID.randomUUID() + ".jpg");
        storageRef.putFile(uri).addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    etCategoryImage.setText(downloadUri.toString());
                    isUploading = false;
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Tải ảnh thành công", Toast.LENGTH_SHORT).show();
                })
        ).addOnFailureListener(e -> {
            isUploading = false;
            btnSave.setEnabled(true);
            Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveCategory() {
        if (isUploading) {
            Toast.makeText(this, "Vui lòng đợi ảnh tải lên xong", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etCategoryName.getText().toString().trim();
        String imageUrl = etCategoryImage.getText().toString().trim();
        boolean showOnHome = switchShowOnHome.isChecked();
        int sortOrder = 0;
        try {
            String sortStr = etSortOrder.getText().toString().trim();
            if (!sortStr.isEmpty()) sortOrder = Integer.parseInt(sortStr);
        } catch (NumberFormatException ignored) {}

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode && editingCategory != null) {
            editingCategory.setName(name);
            editingCategory.setImageUrl(imageUrl);
            editingCategory.setParentId(selectedParentId);
            editingCategory.setShowOnHome(showOnHome);
            editingCategory.setSortOrder(sortOrder);
            categoryDAO.updateCategory(editingCategory).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show();
                resetForm();
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Category newCat = new Category();
            newCat.setName(name);
            newCat.setImageUrl(imageUrl);
            newCat.setParentId(selectedParentId);
            newCat.setShowOnHome(showOnHome);
            newCat.setSortOrder(sortOrder);
            categoryDAO.addCategory(newCat).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Đã thêm danh mục mới!", Toast.LENGTH_SHORT).show();
                resetForm();
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Lỗi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showDeleteConfirmation(Category category) {
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
}
