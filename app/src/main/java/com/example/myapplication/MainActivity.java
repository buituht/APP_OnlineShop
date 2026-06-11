package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;
    private BannerDAO bannerDAO;
    private FaqDAO faqDAO;
    private RecommendationHelper recommendationHelper;
    private GeminiRecommendationService geminiService;
    
    private List<Product> productList;
    private List<Product> filteredList;
    private ProductAdapter productAdapter;

    private List<Product> hotDiscountList;
    private ProductAdapter hotDiscountAdapter;
    private NonScrollListView lvHotDiscount;

    private List<Product> newArrivalList;
    private ProductAdapter newArrivalAdapter;
    private NonScrollListView lvNewArrival;

    private List<Product> recommendedList;
    private ProductAdapter recommendedAdapter;
    private NonScrollListView lvRecommendations;
    private TextView tvTitleRecommendations;

    private List<Product> recentList;
    private ProductAdapter recentAdapter;
    private NonScrollListView lvRecent;
    private TextView tvTitleRecent;

    private List<Category> categoryList;
    private List<Category> displayCategoryList = new ArrayList<>();
    private List<String> currentCategoryFilter = new ArrayList<>();
    private List<Category> allCategoryList = new ArrayList<>();
    private HomeCategoryAdapter categoryAdapter;
    private RecyclerView rvCategories;

    private List<Faq> faqList;
    private FaqAdapter faqAdapter;
    private RecyclerView rvFaqs;

    private List<Banner> bannerList;
    private BannerAdapter bannerAdapter;
    private ViewPager2 vpBanners;
    private Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRunnable;

    public static boolean isLoggedIn = false;
    public static boolean isAdmin = false;
    public static User currentUser = null;

    private Button btnLoginNav;
    private ImageButton btnLogout;
    private ImageView btnVoiceSearch;
    private ImageView btnSearchIcon;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNavigationView;

    private EditText etSearch;
    private TextView tvCartCount, tvCountdown;
    private NonScrollListView lvProducts;

    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String query = matches.get(0);
                        navigateToSearch(query);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);

        productDAO = new ProductDAO(this);
        categoryDAO = new CategoryDAO(this);
        bannerDAO = new BannerDAO(this);
        faqDAO = new FaqDAO(this);
        recommendationHelper = new RecommendationHelper(this);
        geminiService = new GeminiRecommendationService();

        initViews();
        setupDataLists();
        setupAdapters();
        setupClickListeners();
        setupBottomNavigation();
        setupSearch();
        setupVoiceSearch();
        setupFlashSaleCountdown();
        
        loadProductsFromFirebase(); 
        loadCategoriesFromFirebase();
        loadBannersFromFirebase();
        loadFaqsFromFirebase();
        updateUIBasedOnLoginStatus();

        AdminCreator.createAdminAccount();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        tvCartCount = findViewById(R.id.tv_cart_count);
        tvCountdown = findViewById(R.id.tv_countdown);
        btnLoginNav = findViewById(R.id.btn_login_nav);
        btnLogout = findViewById(R.id.btn_logout);
        fabAdd = findViewById(R.id.fab_add);
        lvProducts = findViewById(R.id.lv_items);
        lvHotDiscount = findViewById(R.id.lv_hot_discount);
        lvNewArrival = findViewById(R.id.lv_new_arrival);


        lvRecommendations = findViewById(R.id.lv_recommendations);
        tvTitleRecommendations = findViewById(R.id.tv_title_recommendations);
        lvRecent = findViewById(R.id.lv_recent);
        tvTitleRecent = findViewById(R.id.tv_title_recent);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        rvCategories = findViewById(R.id.rv_categories);
        rvFaqs = findViewById(R.id.rv_news);
        vpBanners = findViewById(R.id.vp_banners);

        btnVoiceSearch = findViewById(R.id.btn_voice_search);
        btnSearchIcon = findViewById(R.id.btn_search_icon);
        findViewById(R.id.btn_cart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
    }

    private void loadBannersFromFirebase() {
        bannerDAO.getAllBanners().addOnSuccessListener(queryDocumentSnapshots -> {
            bannerList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Banner banner = doc.toObject(Banner.class);
                if (banner != null) bannerList.add(banner);
            }
            bannerAdapter.notifyDataSetChanged();
            setupAutoSlide();
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading banners", e));
    }

    private void setupAutoSlide() {
        if (bannerRunnable != null) bannerHandler.removeCallbacks(bannerRunnable);
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerList.size() > 0) {
                    int nextItem = (vpBanners.getCurrentItem() + 1) % bannerList.size();
                    vpBanners.setCurrentItem(nextItem, true);
                    bannerHandler.postDelayed(this, 3000);
                }
            }
        };
        bannerHandler.postDelayed(bannerRunnable, 3000);
    }

    private void loadFaqsFromFirebase() {
        faqDAO.getAllFaqs().addOnSuccessListener(queryDocumentSnapshots -> {
            faqList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Faq faq = doc.toObject(Faq.class);
                if (faq != null) faqList.add(faq);
            }
            faqAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading FAQs", e));
    }

    private void setupFlashSaleCountdown() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            int seconds = 7200 + 900 + 45;
            @Override
            public void run() {
                int h = seconds / 3600;
                int m = (seconds % 3600) / 60;
                int s = seconds % 60;
                if (tvCountdown != null) tvCountdown.setText(String.format("%02d : %02d : %02d", h, m, s));
                if (seconds > 0) {
                    seconds--;
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    private void loadCategoriesFromFirebase() {
        categoryDAO.getAllCategories().addOnSuccessListener(queryDocumentSnapshots -> {
            allCategoryList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Category cat = doc.toObject(Category.class);
                if (cat != null) { cat.setId(doc.getId()); allCategoryList.add(cat); }
            }
            showParentCategories();
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading categories", e));
    }

    private void showParentCategories() {
        displayCategoryList.clear();
        displayCategoryList.add(new Category("all", "Tất cả", ""));
        for (Category c : allCategoryList) {
            if (c.isShowOnHome()) displayCategoryList.add(c);
        }
        if (categoryAdapter != null) { categoryAdapter.setSelectedPosition(0); categoryAdapter.notifyDataSetChanged(); }
        filterByCategory("Tất cả");
    }

    private void showSubCategories(String parentId, String parentName) {
        displayCategoryList.clear();
        displayCategoryList.add(new Category("back", "Quay lại", ""));
        for (Category c : allCategoryList) {
            if (parentId.equals(c.getParentId())) displayCategoryList.add(c);
        }
        if (categoryAdapter != null) { categoryAdapter.setSelectedPosition(0); categoryAdapter.notifyDataSetChanged(); }
        filterByCategoryHierarchy(parentId, parentName);
    }

    private void onCategoryClicked(Category c) {
        ArrayList<String> categoryNames = new ArrayList<>();
        if (!c.getId().equals("all")) {
            categoryNames.add(c.getName());
            for (Category child : allCategoryList) {
                if (c.getId().equals(child.getParentId())) categoryNames.add(child.getName());
            }
        }
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putStringArrayListExtra("category_names", categoryNames);
        intent.putExtra("category_title", c.getId().equals("all") ? "Tất cả sản phẩm" : c.getName());
        startActivity(intent);
    }

    private void filterByCategoryHierarchy(String pId, String pName) {
        currentCategoryFilter.clear();
        currentCategoryFilter.add(pName);
        for (Category c : allCategoryList) {
            if (pId.equals(c.getParentId())) currentCategoryFilter.add(c.getName());
        }
        filterProducts(etSearch.getText().toString());
    }

    private void filterByCategory(String name) {
        currentCategoryFilter.clear();
        if (!name.isEmpty() && !name.equalsIgnoreCase("Tất cả")) {
            currentCategoryFilter.add(name);
        }
        filterProducts(etSearch.getText().toString());
    }

    private void setupDataLists() {
        productList = new ArrayList<>(); filteredList = new ArrayList<>(); hotDiscountList = new ArrayList<>();
        newArrivalList = new ArrayList<>(); recommendedList = new ArrayList<>(); recentList = new ArrayList<>();
        bannerList = new ArrayList<>(); faqList = new ArrayList<>();
    }

    private void setupAdapters() {
        ProductAdapter.OnProductActionListener action = new ProductAdapter.OnProductActionListener() {
            @Override public void onEdit(Product p) {} @Override public void onDelete(Product p) {}
            @Override public void onBuy(Product p) { CartActivity.cartItemList.add(p); updateCartBadge(); Toast.makeText(MainActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show(); }
            @Override public void onItemClick(Product p) { recommendationHelper.recordProductView(p); Intent i = new Intent(MainActivity.this, ProductDetailActivity.class); i.putExtra("product_data", p); startActivity(i); }
        };
        productAdapter = new ProductAdapter(this, filteredList, action); lvProducts.setAdapter(productAdapter);
        hotDiscountAdapter = new ProductAdapter(this, hotDiscountList, action); lvHotDiscount.setAdapter(hotDiscountAdapter);
        newArrivalAdapter = new ProductAdapter(this, newArrivalList, action); lvNewArrival.setAdapter(newArrivalAdapter);
        recommendedAdapter = new ProductAdapter(this, recommendedList, action); lvRecommendations.setAdapter(recommendedAdapter);
        recentAdapter = new ProductAdapter(this, recentList, action); lvRecent.setAdapter(recentAdapter);
        categoryAdapter = new HomeCategoryAdapter(displayCategoryList, this::onCategoryClicked);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
        bannerAdapter = new BannerAdapter(bannerList); vpBanners.setAdapter(bannerAdapter);
        faqAdapter = new FaqAdapter(this, faqList, faq -> new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(faq.getQuestion()).setMessage(faq.getAnswer()).show());
        rvFaqs.setLayoutManager(new LinearLayoutManager(this));
        rvFaqs.setAdapter(faqAdapter);
    }

    private void updateCartBadge() { if (tvCartCount != null) tvCartCount.setText(String.valueOf(CartActivity.cartItemList.size())); }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterProducts(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        if (btnSearchIcon != null) {
            btnSearchIcon.setOnClickListener(v -> {
                if (etSearch.getText().toString().trim().isEmpty()) {
                    etSearch.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    performSearch();
                }
            });
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        navigateToSearch(query);
    }

    private void navigateToSearch(String query) {
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("search_query", query);
        intent.putExtra("category_title", "Kết quả: \"" + query + "\"");
        startActivity(intent);
    }

    private void setupVoiceSearch() {
        if (btnVoiceSearch == null) return;
        btnVoiceSearch.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói tên sản phẩm cần tìm...");
            try {
                speechLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterProducts(String q) {
        filteredList.clear();
        for (Product p : productList) {
            boolean matchesCategory = currentCategoryFilter.isEmpty();
            if (!matchesCategory && p.getCategory() != null) {
                for (String n : currentCategoryFilter) {
                    if (p.getCategory().equalsIgnoreCase(n)) { matchesCategory = true; break; }
                }
            }
            boolean matchesSearch = q.isEmpty() ||
                    (p.getName() != null && p.getName().toLowerCase().contains(q.toLowerCase()));
            if (matchesCategory && matchesSearch) filteredList.add(p);
        }
        productAdapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        android.view.Menu menu = bottomNavigationView.getMenu();
        if (menu.findItem(R.id.nav_register) != null)
            menu.findItem(R.id.nav_register).setVisible(!isLoggedIn);
        if (menu.findItem(R.id.nav_favorites) != null)
            menu.findItem(R.id.nav_favorites).setVisible(isLoggedIn);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            if (itemId == R.id.nav_products) { startActivity(new Intent(this, ProductListActivity.class)); return true; }
            if (itemId == R.id.nav_register) { startActivity(new Intent(this, RegisterActivity.class)); return true; }
            if (itemId == R.id.nav_favorites) { startActivity(new Intent(this, FavoriteActivity.class)); return true; }
            if (itemId == R.id.nav_profile) {
                if (isLoggedIn) startActivity(new Intent(this, ProfileActivity.class));
                else startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        btnLoginNav.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        btnLogout.setOnClickListener(v -> {
            isLoggedIn = false; isAdmin = false; currentUser = null;
            updateUIBasedOnLoginStatus();
            setupBottomNavigation();
        });
    }

    private void updateUIBasedOnLoginStatus() {
        if (isLoggedIn) { btnLoginNav.setVisibility(View.GONE); btnLogout.setVisibility(View.VISIBLE); }
        else { btnLoginNav.setVisibility(View.VISIBLE); btnLogout.setVisibility(View.GONE); }
        if (productAdapter != null) productAdapter.notifyDataSetChanged();
        if (hotDiscountAdapter != null) hotDiscountAdapter.notifyDataSetChanged();
        if (newArrivalAdapter != null) newArrivalAdapter.notifyDataSetChanged();
        if (recommendedAdapter != null) recommendedAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIBasedOnLoginStatus();
        setupBottomNavigation();
        loadProductsFromFirebase();
        loadCategoriesFromFirebase();
        loadBannersFromFirebase();
        loadFaqsFromFirebase();
        updateCartBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bannerRunnable != null) bannerHandler.removeCallbacks(bannerRunnable);
    }

    private void loadProductsFromFirebase() {
        productDAO.getAllProducts().addOnSuccessListener(queryDocumentSnapshots -> {
            productList.clear(); hotDiscountList.clear(); newArrivalList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Product p = doc.toObject(Product.class);
                if (p != null) {
                    p.setId(doc.getId()); productList.add(p);
                    if (p.isHotDiscount()) hotDiscountList.add(p);
                    if (p.isNewArrival()) newArrivalList.add(p);
                }
            }
            if (findViewById(R.id.tv_title_hot_discount) != null) findViewById(R.id.tv_title_hot_discount).setVisibility(hotDiscountList.isEmpty() ? View.GONE : View.VISIBLE);
            if (findViewById(R.id.tv_title_new_arrival) != null) findViewById(R.id.tv_title_new_arrival).setVisibility(newArrivalList.isEmpty() ? View.GONE : View.VISIBLE);
            hotDiscountAdapter.notifyDataSetChanged();
            newArrivalAdapter.notifyDataSetChanged();
            loadRecommendations();
            filterProducts(etSearch.getText().toString());
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading products", e));
    }

    private void loadRecommendations() {
        List<String> viewedIds = recommendationHelper.getViewedProductIds();
        if (viewedIds == null || viewedIds.isEmpty()) {
            loadLegacyRecommendations();
            return;
        }

        List<Product> viewedHistory = new ArrayList<>();
        for (String id : viewedIds) {
            for (Product p : productList) {
                if (p.getId().equals(id)) {
                    viewedHistory.add(p);
                    break;
                }
            }
        }

        tvTitleRecommendations.setText("Gợi ý riêng cho bạn (AI)");
        geminiService.getRecommendations(viewedHistory, productList, new GeminiRecommendationService.RecommendationCallback() {
            @Override
            public void onRecommendationReceived(List<String> recommendedProductIds) {
                runOnUiThread(() -> {
                    recommendedList.clear();
                    for (String id : recommendedProductIds) {
                        for (Product p : productList) {
                            if (p.getId().equals(id)) {
                                recommendedList.add(p);
                                break;
                            }
                        }
                    }
                    updateRecommendationUI();
                });
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Gemini Error, fallback to legacy", t);
                runOnUiThread(() -> loadLegacyRecommendations());
            }
        });
    }

    private void loadLegacyRecommendations() {
        tvTitleRecommendations.setText("Gợi ý riêng cho bạn");
        String favCategory = recommendationHelper.getMostInterestedCategory();
        recommendedList.clear();
        if (favCategory != null && !favCategory.isEmpty()) {
            for (Product p : productList) {
                if (favCategory.equalsIgnoreCase(p.getCategory())) recommendedList.add(p);
                if (recommendedList.size() >= 6) break;
            }
        }
        updateRecommendationUI();
    }

    private void updateRecommendationUI() {
        if (recommendedList.isEmpty()) {
            tvTitleRecommendations.setVisibility(View.GONE);
            lvRecommendations.setVisibility(View.GONE);
        } else {
            tvTitleRecommendations.setVisibility(View.VISIBLE);
            lvRecommendations.setVisibility(View.VISIBLE);
            recommendedAdapter.notifyDataSetChanged();
        }
    }
}
