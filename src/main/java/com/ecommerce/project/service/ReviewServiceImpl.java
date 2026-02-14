package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Review;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.ReviewDTO;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.repositories.ReviewRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @CacheEvict(value = "productReviewStats", key = "#productId")
    public ReviewDTO createReview(Long productId, ReviewDTO reviewDTO) {
        User currentUser = authUtil.loggedInUser();

        if (reviewRepository.existsByUserUserIdAndProductProductId(currentUser.getUserId(), productId)) {
            throw new APIException("You have already reviewed this product. You can update your existing review.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        Review review = new Review();
        review.setRating(reviewDTO.getRating());
        review.setTitle(reviewDTO.getTitle());
        review.setComment(reviewDTO.getComment());
        review.setUser(currentUser);
        review.setProduct(product);
        review.setHelpfulVotes(0);
        review.setVerifiedPurchase(hasUserPurchasedProduct(currentUser.getUserId(), productId));

        Review savedReview = reviewRepository.save(review);
        return mapToDTO(savedReview);
    }

    @Override
    @Cacheable(value = "productReviews", key = "#productId + '_' + #page + '_' + #size + '_' + #sortBy")
    public Page<ReviewDTO> getProductReviews(Long productId, int page, int size, String sortBy) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        Sort sort = switch (sortBy.toLowerCase()) {
            case "helpful" -> Sort.by(Sort.Direction.DESC, "helpfulVotes");
            case "rating_high" -> Sort.by(Sort.Direction.DESC, "rating");
            case "rating_low" -> Sort.by(Sort.Direction.ASC, "rating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviews = reviewRepository.findByProductProductId(productId, pageable);

        return reviews.map(this::mapToDTO);
    }

    @Override
    public ReviewDTO getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));
        return mapToDTO(review);
    }

    @Override
    @CacheEvict(value = {"productReviews", "productReviewStats"}, allEntries = true)
    public ReviewDTO updateReview(Long reviewId, ReviewDTO reviewDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));

        User currentUser = authUtil.loggedInUser();
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new APIException("You can only update your own reviews");
        }

        review.setRating(reviewDTO.getRating());
        review.setTitle(reviewDTO.getTitle());
        review.setComment(reviewDTO.getComment());

        Review updatedReview = reviewRepository.save(review);
        return mapToDTO(updatedReview);
    }

    @Override
    @CacheEvict(value = {"productReviews", "productReviewStats"}, allEntries = true)
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));

        User currentUser = authUtil.loggedInUser();
        boolean isAuthor = review.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleName().name().equals("ROLE_ADMIN"));

        if (!isAuthor && !isAdmin) {
            throw new APIException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    @Override
    @Cacheable(value = "productReviewStats", key = "#productId")
    public Map<String, Object> getProductReviewStats(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        Map<String, Object> stats = new HashMap<>();

        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        long totalReviews = reviewRepository.countByProductProductId(productId);
        stats.put("totalReviews", totalReviews);

        List<Object[]> distribution = reviewRepository.getRatingDistributionByProductId(productId);
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDistribution.put(rating, count);
        }
        stats.put("ratingDistribution", ratingDistribution);

        return stats;
    }

    @Override
    public void markReviewHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));

        review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        reviewRepository.save(review);
    }

    @Override
    public ReviewDTO getUserReviewForProduct(Long productId) {
        User currentUser = authUtil.loggedInUser();
        return reviewRepository.findByUserUserIdAndProductProductId(currentUser.getUserId(), productId)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Override
    public boolean hasUserReviewedProduct(Long productId) {
        User currentUser = authUtil.loggedInUser();
        return reviewRepository.existsByUserUserIdAndProductProductId(currentUser.getUserId(), productId);
    }

    private boolean hasUserPurchasedProduct(Long userId, Long productId) {
        return false;
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = modelMapper.map(review, ReviewDTO.class);
        dto.setUserName(review.getUser().getUserName());
        dto.setUserId(review.getUser().getUserId());
        dto.setProductId(review.getProduct().getProductId());
        dto.setProductName(review.getProduct().getProductName());
        return dto;
    }
}
