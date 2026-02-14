package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.model.Wishlist;
import com.ecommerce.project.model.WishlistItem;
import com.ecommerce.project.payload.WishlistDTO;
import com.ecommerce.project.payload.WishlistDTO.WishlistItemDTO;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.repositories.WishlistItemRepository;
import com.ecommerce.project.repositories.WishlistRepository;
import com.ecommerce.project.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthUtil authUtil;

    @Override
    public WishlistDTO getWishlist() {
        User user = authUtil.loggedInUser();
        Wishlist wishlist = getOrCreateWishlist(user);
        return mapToDTO(wishlist);
    }

    @Override
    public WishlistDTO addToWishlist(Long productId) {
        User user = authUtil.loggedInUser();
        Wishlist wishlist = getOrCreateWishlist(user);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (wishlistItemRepository.existsByWishlistWishlistIdAndProductProductId(wishlist.getWishlistId(), productId)) {
            throw new APIException("Product is already in your wishlist");
        }

        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setProduct(product);
        item.setPriceWhenAdded(product.getSpecialPrice());

        wishlistItemRepository.save(item);
        wishlist.getItems().add(item);

        return mapToDTO(wishlist);
    }

    @Override
    public WishlistDTO removeFromWishlist(Long productId) {
        User user = authUtil.loggedInUser();
        Wishlist wishlist = getOrCreateWishlist(user);

        if (!wishlistItemRepository.existsByWishlistWishlistIdAndProductProductId(wishlist.getWishlistId(), productId)) {
            throw new APIException("Product is not in your wishlist");
        }

        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getWishlistId(), productId);
        wishlist.getItems().removeIf(item -> item.getProduct().getProductId().equals(productId));

        return mapToDTO(wishlist);
    }

    @Override
    public boolean isProductInWishlist(Long productId) {
        User user = authUtil.loggedInUser();
        Wishlist wishlist = wishlistRepository.findByUser(user).orElse(null);

        if (wishlist == null) {
            return false;
        }

        return wishlistItemRepository.existsByWishlistWishlistIdAndProductProductId(wishlist.getWishlistId(), productId);
    }

    @Override
    public void moveToCart(Long productId, Integer quantity) {
        User user = authUtil.loggedInUser();
        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseThrow(() -> new APIException("Wishlist not found"));

        if (!wishlistItemRepository.existsByWishlistWishlistIdAndProductProductId(wishlist.getWishlistId(), productId)) {
            throw new APIException("Product is not in your wishlist");
        }

        cartService.addProductToCart(productId, quantity);
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getWishlistId(), productId);
    }

    @Override
    public void clearWishlist() {
        User user = authUtil.loggedInUser();
        Wishlist wishlist = wishlistRepository.findByUser(user).orElse(null);

        if (wishlist != null) {
            wishlist.getItems().clear();
            wishlistRepository.save(wishlist);
        }
    }

    private Wishlist getOrCreateWishlist(User user) {
        return wishlistRepository.findByUserWithItems(user)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUser(user);
                    return wishlistRepository.save(newWishlist);
                });
    }

    private WishlistDTO mapToDTO(Wishlist wishlist) {
        WishlistDTO dto = new WishlistDTO();
        dto.setWishlistId(wishlist.getWishlistId());
        dto.setUserId(wishlist.getUser().getUserId());
        dto.setCreatedAt(wishlist.getCreatedAt());
        dto.setUpdatedAt(wishlist.getUpdatedAt());

        dto.setItems(wishlist.getItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList()));

        dto.setTotalItems(dto.getItems().size());

        return dto;
    }

    private WishlistItemDTO mapItemToDTO(WishlistItem item) {
        Product product = item.getProduct();

        WishlistItemDTO dto = new WishlistItemDTO();
        dto.setId(item.getId());
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setProductImage(product.getImage());
        dto.setCurrentPrice(product.getSpecialPrice());
        dto.setPriceWhenAdded(item.getPriceWhenAdded());
        dto.setPriceDifference(product.getSpecialPrice() - item.getPriceWhenAdded());
        dto.setInStock(product.getQuantity() > 0);
        dto.setAddedAt(item.getAddedAt());

        return dto;
    }
}
