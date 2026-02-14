package com.ecommerce.project.service;

import com.ecommerce.project.payload.ReviewDTO;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ReviewService {

    ReviewDTO createReview(Long productId, ReviewDTO reviewDTO);

    Page<ReviewDTO> getProductReviews(Long productId, int page, int size, String sortBy);

    ReviewDTO getReviewById(Long reviewId);

    ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO);

    void deleteReview(Long reviewId);

    Map<String, Object> getProductReviewStats(Long productId);

    void markReviewHelpful(Long reviewId);

    ReviewDTO getUserReviewForProduct(Long productId);

    boolean hasUserReviewedProduct(Long productId);
}
