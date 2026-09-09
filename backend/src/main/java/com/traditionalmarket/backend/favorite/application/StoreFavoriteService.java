package com.traditionalmarket.backend.favorite.application;

import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.traditionalmarket.backend.favorite.domain.StoreFavorite;
import com.traditionalmarket.backend.favorite.dto.response.FavoriteStatusResponse;
import com.traditionalmarket.backend.favorite.dto.response.FavoriteStoreResponse;
import com.traditionalmarket.backend.favorite.repository.StoreFavoriteRepository;
import com.traditionalmarket.backend.store.domain.Store;
import com.traditionalmarket.backend.store.exception.StoreNotFoundException;
import com.traditionalmarket.backend.store.repository.StoreRepository;
import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFavoriteService {

    private final StoreFavoriteRepository storeFavoriteRepository;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;

    public List<FavoriteStoreResponse> getFavorites(String email) {
        UserAccount user = getUser(email);

        return storeFavoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(FavoriteStoreResponse::from)
                .toList();
    }

    public FavoriteStatusResponse getFavoriteStatus(String email, Long storeId) {
        UserAccount user = getUser(email);
        ensureStoreExists(storeId);

        return new FavoriteStatusResponse(
                storeFavoriteRepository.existsByUserIdAndStoreId(user.getId(), storeId)
        );
    }

    @Transactional
    public FavoriteStatusResponse addFavorite(String email, Long storeId) {
        UserAccount user = getUserForUpdate(email);
        Store store = getStore(storeId);

        if (!storeFavoriteRepository.existsByUserIdAndStoreId(user.getId(), storeId)) {
            storeFavoriteRepository.save(StoreFavorite.create(user, store));
        }

        return new FavoriteStatusResponse(true);
    }

    @Transactional
    public void removeFavorite(String email, Long storeId) {
        UserAccount user = getUserForUpdate(email);
        ensureStoreExists(storeId);
        storeFavoriteRepository.deleteByUserIdAndStoreId(user.getId(), storeId);
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private UserAccount getUserForUpdate(String email) {
        return userAccountRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 회원을 찾을 수 없습니다."));
    }

    private Store getStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    private void ensureStoreExists(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(storeId);
        }
    }
}
