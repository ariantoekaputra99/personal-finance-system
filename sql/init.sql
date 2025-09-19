-- Database initialization script
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    description TEXT,
    color VARCHAR(7), -- hex color code
    icon VARCHAR(50),
    type VARCHAR(20) CHECK (type IN ('INCOME', 'EXPENSE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Accounts table
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) CHECK (type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT')),
    balance DECIMAL(15,2) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'IDR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    type VARCHAR(20) CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    description TEXT,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tags TEXT[], -- PostgreSQL array for tags
    location VARCHAR(255),
    receipt_url VARCHAR(500),
    is_recurring BOOLEAN DEFAULT false,
    recurring_pattern VARCHAR(20) -- DAILY, WEEKLY, MONTHLY, YEARLY
);

-- Budgets table
CREATE TABLE budgets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    period VARCHAR(20) CHECK (period IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Goals table
CREATE TABLE financial_goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    target_amount DECIMAL(15,2) NOT NULL,
    current_amount DECIMAL(15,2) DEFAULT 0.00,
    target_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_achieved BOOLEAN DEFAULT false
);

-- Notifications table (updated for notification service)
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    category VARCHAR(50)
);

-- Indexes for performance
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_user_read_type ON notifications(user_id, is_read, type);

-- Insert default categories
INSERT INTO categories (name, description, color, icon, type) VALUES
-- Income categories
('Salary', 'Monthly salary income', '#4CAF50', 'work', 'INCOME'),
('Freelance', 'Freelance work income', '#8BC34A', 'laptop', 'INCOME'),
('Investment', 'Investment returns', '#2196F3', 'trending_up', 'INCOME'),
('Business', 'Business income', '#FF9800', 'business', 'INCOME'),
('Other Income', 'Other sources of income', '#9C27B0', 'attach_money', 'INCOME'),

-- Expense categories
('Food & Dining', 'Restaurant, groceries, food delivery', '#F44336', 'restaurant', 'EXPENSE'),
('Transportation', 'Gas, public transport, taxi', '#607D8B', 'directions_car', 'EXPENSE'),
('Shopping', 'Clothing, electronics, general shopping', '#E91E63', 'shopping_cart', 'EXPENSE'),
('Entertainment', 'Movies, games, hobbies', '#9C27B0', 'movie', 'EXPENSE'),
('Bills & Utilities', 'Electricity, water, internet, phone', '#FF5722', 'receipt', 'EXPENSE'),
('Healthcare', 'Medical expenses, insurance', '#00BCD4', 'local_hospital', 'EXPENSE'),
('Education', 'Books, courses, tuition', '#3F51B5', 'school', 'EXPENSE'),
('Travel', 'Vacation, business trips', '#009688', 'flight', 'EXPENSE'),
('Home & Garden', 'Rent, mortgage, home improvement', '#795548', 'home', 'EXPENSE'),
('Personal Care', 'Haircut, cosmetics, gym', '#FFC107', 'face', 'EXPENSE');

-- Create materialized view for analytics
CREATE MATERIALIZED VIEW monthly_spending_summary AS
SELECT 
    user_id,
    DATE_TRUNC('month', transaction_date) as month,
    c.name as category_name,
    c.type as category_type,
    SUM(amount) as total_amount,
    COUNT(*) as transaction_count,
    AVG(amount) as avg_amount
FROM transactions t
LEFT JOIN categories c ON t.category_id = c.name
WHERE t.type IN ('INCOME', 'EXPENSE')
GROUP BY user_id, DATE_TRUNC('month', transaction_date), c.name, c.type;

-- Create index on materialized view
CREATE INDEX idx_monthly_summary_user_month ON monthly_spending_summary(user_id, month);

-- Function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_monthly_summary()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY monthly_spending_summary;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-refresh materialized view
CREATE TRIGGER trigger_refresh_monthly_summary
    AFTER INSERT OR UPDATE OR DELETE ON transactions
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_monthly_summary();

-- Function to update account balance
CREATE OR REPLACE FUNCTION update_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE accounts 
        SET balance = balance + 
            CASE 
                WHEN NEW.type = 'INCOME' THEN NEW.amount
                WHEN NEW.type = 'EXPENSE' THEN -NEW.amount
                ELSE 0
            END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.account_id;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        -- Revert old transaction
        UPDATE accounts 
        SET balance = balance - 
            CASE 
                WHEN OLD.type = 'INCOME' THEN OLD.amount
                WHEN OLD.type = 'EXPENSE' THEN -OLD.amount
                ELSE 0
            END
        WHERE id = OLD.account_id;
        
        -- Apply new transaction
        UPDATE accounts 
        SET balance = balance + 
            CASE 
                WHEN NEW.type = 'INCOME' THEN NEW.amount
                WHEN NEW.type = 'EXPENSE' THEN -NEW.amount
                ELSE 0
            END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.account_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE accounts 
        SET balance = balance - 
            CASE 
                WHEN OLD.type = 'INCOME' THEN OLD.amount
                WHEN OLD.type = 'EXPENSE' THEN -OLD.amount
                ELSE 0
            END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = OLD.account_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger for account balance updates
CREATE TRIGGER trigger_update_account_balance
    AFTER INSERT OR UPDATE OR DELETE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_account_balance();

-- Sample notification data
INSERT INTO notifications (user_id, title, message, type, priority, category, is_read) VALUES
('cd7f4f51-c893-4153-a4d3-6d8b1d9b57d5', 'Welcome to Finance App', 'Your account has been successfully created', 'WELCOME', 'LOW', 'SYSTEM', false),
('cd7f4f51-c893-4153-a4d3-6d8b1d9b57d5', 'Transaction Alert', 'New large transaction has been processed', 'TRANSACTION_UPDATE', 'MEDIUM', 'TRANSACTION', false),
('cd7f4f51-c893-4153-a4d3-6d8b1d9b57d5', 'Budget Alert', 'You are approaching your monthly budget limit', 'BUDGET_ALERT', 'HIGH', 'BUDGET', false),
('cd7f4f51-c893-4153-a4d3-6d8b1d9b57d5', 'Monthly Report Ready', 'Your financial report for this month is available', 'REPORT_READY', 'LOW', 'REPORT', true);

-- Function to create notification
CREATE OR REPLACE FUNCTION create_notification(
    p_user_id UUID,
    p_title VARCHAR(255),
    p_message TEXT,
    p_type VARCHAR(50),
    p_priority VARCHAR(20) DEFAULT 'MEDIUM',
    p_category VARCHAR(50) DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    notification_id UUID;
BEGIN
    INSERT INTO notifications (user_id, title, message, type, priority, category)
    VALUES (p_user_id, p_title, p_message, p_type, p_priority, p_category)
    RETURNING id INTO notification_id;
    
    RETURN notification_id;
END;
$$ LANGUAGE plpgsql;

-- Function to mark notifications as read
CREATE OR REPLACE FUNCTION mark_notifications_read(
    p_user_id UUID,
    p_notification_ids UUID[] DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    IF p_notification_ids IS NULL THEN
        -- Mark all unread notifications as read
        UPDATE notifications 
        SET is_read = true, read_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        WHERE user_id = p_user_id AND is_read = false;
    ELSE
        -- Mark specific notifications as read
        UPDATE notifications 
        SET is_read = true, read_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
        WHERE user_id = p_user_id AND id = ANY(p_notification_ids) AND is_read = false;
    END IF;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;