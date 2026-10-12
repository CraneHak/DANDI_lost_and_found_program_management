package org.example.favorite;

public class FavoriteItemNotFoundException extends RuntimeException {
    public FavoriteItemNotFoundException(Integer lostItemId) {
        super("lost item not found: " + lostItemId);
    }
}
