package com.traditionalmarket.backend.favorite.presentation;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.traditionalmarket.backend.favorite.application.StoreFavoriteService;
import com.traditionalmarket.backend.favorite.dto.response.FavoriteStatusResponse;
import com.traditionalmarket.backend.favorite.dto.response.FavoriteStoreResponse;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/favorites/stores")
@RequiredArgsConstructor
public class StoreFavoriteController {

    private final StoreFavoriteService storeFavoriteService;

    @GetMapping
    public List<FavoriteStoreResponse> getFavorites(Principal principal) {
        return storeFavoriteService.getFavorites(principal.getName());
    }

    @GetMapping("/{storeId}/status")
    public FavoriteStatusResponse getFavoriteStatus(
            Principal principal,
            @PathVariable @Positive Long storeId
    ) {
        return storeFavoriteService.getFavoriteStatus(principal.getName(), storeId);
    }

    @PostMapping("/{storeId}")
    public ResponseEntity<FavoriteStatusResponse> addFavorite(
            Principal principal,
            @PathVariable @Positive Long storeId
    ) {
        FavoriteStatusResponse response = storeFavoriteService.addFavorite(
                principal.getName(),
                storeId
        );

        return ResponseEntity.created(URI.create("/api/favorites/stores/" + storeId))
                .body(response);
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> removeFavorite(
            Principal principal,
            @PathVariable @Positive Long storeId
    ) {
        storeFavoriteService.removeFavorite(principal.getName(), storeId);
        return ResponseEntity.noContent().build();
    }
}
