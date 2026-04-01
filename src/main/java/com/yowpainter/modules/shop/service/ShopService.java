package com.yowpainter.modules.shop.service;

import com.yowpainter.modules.artist.entity.Artist;
import com.yowpainter.modules.artist.repository.ArtistRepository;
import com.yowpainter.modules.artwork.entity.Artwork;
import com.yowpainter.modules.artwork.repository.ArtworkRepository;
import com.yowpainter.modules.auth.entity.AppUser;
import com.yowpainter.modules.auth.repository.AppUserRepository;
import com.yowpainter.modules.shop.dto.*;
import com.yowpainter.modules.shop.entity.*;
import com.yowpainter.modules.shop.repository.OrderRepository;
import com.yowpainter.modules.shop.repository.PaymentRepository;
import com.yowpainter.modules.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtistRepository artistRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ProductResponse createProduct(String artistEmail, ProductCreateRequest request) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();

        Artwork artwork = null;
        if (request.getArtworkId() != null) {
            artwork = artworkRepository.findById(request.getArtworkId()).orElseThrow();
            if (!artwork.getArtistId().equals(artist.getId())) throw new IllegalStateException("Not authorized");
            artwork.setStatus(com.yowpainter.modules.artwork.entity.ArtworkStatus.ON_SALE);
            artworkRepository.save(artwork);
        }

        Product product = Product.builder()
                .artistId(artist.getId())
                .artwork(artwork)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(true)
                .build();

        return mapToProductResponse(productRepository.save(product));
    }

    public List<ProductResponse> getProductsByArtist(UUID artistId) {
        if (artistId == null) {
            // En multi-tenant, si pas d'ID, on prend tout ce qui est actif dans le schéma courant
            return productRepository.findAll().stream()
                    .filter(Product::isActive)
                    .map(this::mapToProductResponse)
                    .collect(Collectors.toList());
        }
        return productRepository.findByArtistIdAndIsActiveTrue(artistId).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse placeOrder(String buyerEmail, OrderCreateRequest request) {
        AppUser buyer = appUserRepository.findByEmail(buyerEmail).orElseThrow();
        Product product = productRepository.findByIdWithPessimisticWriteLock(request.getProductId()).orElseThrow();

        if (!product.isActive() || product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Produit epuise ou quantite insuffisante");
        }

        product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
        productRepository.save(product);

        if (product.getStockQuantity() == 0 && product.getArtwork() != null) {
            Artwork artwork = product.getArtwork();
            artwork.setStatus(com.yowpainter.modules.artwork.entity.ArtworkStatus.SOLD);
            artworkRepository.save(artwork);
        }

        BigDecimal totalPrice = product.getPrice().multiply(new BigDecimal(request.getQuantity()));

        Order order = Order.builder()
                .buyerId(buyer.getId())
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalPrice)
                .build();

        order.addItem(OrderItem.builder().product(product).quantity(request.getQuantity()).unitPrice(product.getPrice()).build());
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    public List<OrderResponse> getMySales(String artistEmail) {
        // En schema-per-tenant, tous les ordres du schema courant appartiennent a l'artiste courant
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getMyPurchases(String buyerEmail) {
        AppUser buyer = appUserRepository.findByEmail(buyerEmail).orElseThrow();
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream()
                .map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));
        return mapToOrderResponse(order);
    }

    public List<ProductResponse> getInventory(String artistEmail) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        return productRepository.findByArtistId(artist.getId()).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

@Transactional
    public void cancelAbandonedOrdersForTenant(String tenantId) {
        log.debug("Cleaning up orders for tenant: {}", tenantId);
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30); 
        List<Order> abandonedOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, threshold);
        
        for (Order order : abandonedOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("[{}] Cancelled abandoned order: {}", tenantId, order.getId());

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                
                if (product.getArtwork() != null) {
                    Artwork artwork = product.getArtwork();
                    if (artwork.getStatus() == com.yowpainter.modules.artwork.entity.ArtworkStatus.SOLD) {
                        artwork.setStatus(com.yowpainter.modules.artwork.entity.ArtworkStatus.ON_SALE);
                        artworkRepository.save(artwork);
                    }
                }
                productRepository.save(product);
            }
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        AppUser buyer = appUserRepository.findById(order.getBuyerId()).orElse(null);
        String buyerName = (buyer != null) ? buyer.getFirstName() + " " + buyer.getLastName() : "Inconnu";

        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .buyerName(buyerName)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(i -> OrderItemResponse.builder()
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .artistId(product.getArtistId())
                .artworkId(product.getArtwork() != null ? product.getArtwork().getId() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .isActive(product.isActive())
                .build();
    }
}
