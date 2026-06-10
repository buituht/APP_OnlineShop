package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private ListView lvFavorites;
    private ProductAdapter adapter;
    private List<Product> favoriteList;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavigationView;
    private UserDAO userDAO;
    private ProductDAO productDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        if (!MainActivity.isLoggedIn || MainActivity.currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem yêu thích!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userDAO = new UserDAO(this);
        productDAO = new ProductDAO(this);

        lvFavorites = findViewById(R.id.gv_favorites);
        tvEmpty = findViewById(R.id.tv_empty_favorites);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        favoriteList = new ArrayList<>();
        adapter = new ProductAdapter(this, favoriteList, new ProductAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {}
            @Override
            public void onDelete(Product product) {}
            @Override
            public void onBuy(Product product) {
                CartActivity.cartItemList.add(product);
                Toast.makeText(FavoriteActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(FavoriteActivity.this, ProductDetailActivity.class);
                intent.putExtra("product_data", product);
                startActivity(intent);
            }
        });
        lvFavorites.setAdapter(adapter);

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        if (MainActivity.currentUser == null) return;
        String email = MainActivity.currentUser.getEmail();
        userDAO.getFavorites(email).addOnSuccessListener(documentSnapshot -> {
            List<String> favIds = new ArrayList<>();
            if (documentSnapshot.exists()) {
                List<?> raw = (List<?>) documentSnapshot.get("favorites");
                if (raw != null) {
                    for (Object o : raw) {
                        if (o != null) favIds.add(o.toString());
                    }
                }
            }
            if (favIds.isEmpty()) {
                favoriteList.clear();
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            productDAO.getAllProducts().addOnSuccessListener(queryDocumentSnapshots -> {
                favoriteList.clear();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    if (favIds.contains(doc.getId())) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            favoriteList.add(p);
                        }
                    }
                }
                tvEmpty.setVisibility(favoriteList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi tải sản phẩm yêu thích", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e("FavoriteActivity", "Lỗi lấy danh sách yêu thích", e);
            Toast.makeText(this, "Lỗi tải danh sách yêu thích", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_favorites);
        android.view.Menu menu = bottomNavigationView.getMenu();
        if (menu.findItem(R.id.nav_register) != null) menu.findItem(R.id.nav_register).setVisible(false);
        if (menu.findItem(R.id.nav_favorites) != null) menu.findItem(R.id.nav_favorites).setVisible(true);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_products) {
                startActivity(new Intent(this, ProductListActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
