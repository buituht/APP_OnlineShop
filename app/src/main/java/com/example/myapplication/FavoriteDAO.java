package com.example.myapplication;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO {
    private final FirebaseFirestore db;

    public FavoriteDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public FavoriteDAO(android.content.Context context) {
        this();
    }

    public Task<Void> addFavorite(String email, String productId) {
        return db.collection("users").document(email)
                .update("favorites", FieldValue.arrayUnion(productId));
    }

    public Task<Void> removeFavorite(String email, String productId) {
        return db.collection("users").document(email)
                .update("favorites", FieldValue.arrayRemove(productId));
    }

    public Task<DocumentSnapshot> getFavorites(String email) {
        return db.collection("users").document(email).get();
    }

    public interface IsFavoriteCallback {
        void onResult(boolean isFavorite);
    }

    public void isFavorite(String email, String productId, IsFavoriteCallback callback) {
        getFavorites(email).addOnSuccessListener(doc -> {
            boolean result = false;
            if (doc.exists()) {
                List<?> favs = (List<?>) doc.get("favorites");
                result = favs != null && favs.contains(productId);
            }
            callback.onResult(result);
        }).addOnFailureListener(e -> callback.onResult(false));
    }

    public void getFavoriteIds(String email, FavoriteIdsCallback callback) {
        getFavorites(email).addOnSuccessListener(doc -> {
            List<String> ids = new ArrayList<>();
            if (doc.exists()) {
                List<?> raw = (List<?>) doc.get("favorites");
                if (raw != null) {
                    for (Object o : raw) {
                        if (o != null) ids.add(o.toString());
                    }
                }
            }
            callback.onResult(ids);
        }).addOnFailureListener(e -> callback.onResult(new ArrayList<>()));
    }

    public interface FavoriteIdsCallback {
        void onResult(List<String> ids);
    }
}
