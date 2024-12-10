CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE status_codes (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description TEXT,
    UNIQUE(entity_type, status)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER',
    avatar_url TEXT,
    status_id INTEGER REFERENCES status_codes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    email_verified BOOLEAN DEFAULT FALSE,
    two_factor_enabled BOOLEAN DEFAULT FALSE
);

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    location VARCHAR(255),
    address TEXT,
    category_id UUID REFERENCES categories(id),
    creator_id UUID REFERENCES users(id),
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    status_id INTEGER REFERENCES status_codes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    registration_start_date TIMESTAMP,
    registration_end_date TIMESTAMP,
    is_public BOOLEAN DEFAULT TRUE,
    cover_image_url TEXT,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8)
);

CREATE TABLE event_tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES events(id),
    tag_name VARCHAR(255) NOT NULL,
    UNIQUE(event_id, tag_name)
);

CREATE TABLE event_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES events(id),
    user_id UUID REFERENCES users(id),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_id INTEGER REFERENCES status_codes(id),
    additional_guests INTEGER DEFAULT 0,
    UNIQUE(event_id, user_id)
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES events(id),
    ticket_type VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2),
    total_quantity INTEGER NOT NULL,
    available_quantity INTEGER NOT NULL,
    sale_start_date TIMESTAMP,
    sale_end_date TIMESTAMP,
    status_id INTEGER REFERENCES status_codes(id),
    max_per_user INTEGER DEFAULT 5,
    description TEXT,
    early_bird_discount DECIMAL(5,2),
    is_free BOOLEAN DEFAULT FALSE
);

CREATE TABLE ticket_purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    ticket_id UUID REFERENCES tickets(id),
    quantity INTEGER NOT NULL,
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_price DECIMAL(10, 2),
    status_id INTEGER REFERENCES status_codes(id),
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    UNIQUE(user_id, ticket_id)
);

CREATE TABLE ticket_qr_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_purchase_id UUID REFERENCES ticket_purchases(id),
    qr_code_data TEXT NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    status_id INTEGER REFERENCES status_codes(id),
    unique_identifier VARCHAR(100) UNIQUE NOT NULL,
    event_id UUID REFERENCES events(id),
    user_id UUID REFERENCES users(id)
);

CREATE TABLE event_discussions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES events(id),
    user_id UUID REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    parent_comment_id UUID REFERENCES event_discussions(id),
    status_id INTEGER REFERENCES status_codes(id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    content TEXT NOT NULL,
    related_event_id UUID REFERENCES events(id),
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_id INTEGER REFERENCES status_codes(id)
);

CREATE INDEX idx_events_creator ON events(creator_id);
CREATE INDEX idx_events_category ON events(category_id);
CREATE INDEX idx_participants_event ON event_participants(event_id);
CREATE INDEX idx_tickets_event ON tickets(event_id);
CREATE INDEX idx_qr_codes_purchase ON ticket_qr_codes(ticket_purchase_id);
CREATE INDEX idx_discussions_event ON event_discussions(event_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);