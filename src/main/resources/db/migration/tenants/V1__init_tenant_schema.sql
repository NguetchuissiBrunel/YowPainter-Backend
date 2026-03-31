-- Migration pour les schémas de tenants (Artistes individuels)
-- Ce script sera exécuté à chaque nouvel enregistrement d'artiste

CREATE TABLE IF NOT EXISTS artwork (
    id UUID PRIMARY KEY,
    artist_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    technique VARCHAR(100),
    style VARCHAR(100),
    dimensions VARCHAR(100),
    tags JSONB,
    status VARCHAR(50) NOT NULL,
    view_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    published_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS artwork_image (
    id UUID PRIMARY KEY,
    artwork_id UUID NOT NULL REFERENCES artwork(id) ON DELETE CASCADE,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS artwork_comment (
    id UUID PRIMARY KEY,
    artwork_id UUID NOT NULL REFERENCES artwork(id) ON DELETE CASCADE,
    user_id UUID NOT NULL, -- Reference to public.app_user(id) - No FK for cross-schema in simple migrations
    content TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS artwork_like (
    id UUID PRIMARY KEY,
    artwork_id UUID NOT NULL REFERENCES artwork(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (artwork_id, user_id)
);

CREATE TABLE IF NOT EXISTS product (
    id UUID PRIMARY KEY,
    artist_id UUID NOT NULL,
    artwork_id UUID REFERENCES artwork(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event (
    id UUID PRIMARY KEY,
    artist_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    poster_url VARCHAR(255),
    start_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    location VARCHAR(255),
    type VARCHAR(50),
    max_capacity INTEGER DEFAULT 0,
    reserved_count INTEGER DEFAULT 0,
    ticket_price DECIMAL(10, 2) DEFAULT 0.00,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS reservation (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_id UUID,
    reserved_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservation(id) ON DELETE CASCADE,
    qr_code_data VARCHAR(255) NOT NULL UNIQUE,
    qr_code_image_url VARCHAR(255),
    is_scanned BOOLEAN DEFAULT FALSE,
    scanned_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscription (
    id UUID PRIMARY KEY,
    artist_id UUID NOT NULL UNIQUE,
    plan VARCHAR(50) NOT NULL,
    start_date TIMESTAMP WITHOUT TIME ZONE,
    end_date TIMESTAMP WITHOUT TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shop_order (
    id UUID PRIMARY KEY,
    buyer_id UUID NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    shipping_address VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES shop_order(id) ON DELETE CASCADE,
    product_id UUID REFERENCES product(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    reference_id UUID NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'EUR',
    status VARCHAR(50) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
