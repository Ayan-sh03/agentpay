package payments

# Default decision is to deny
default allow = false

# Allow purchases under spend cap
allow {
    input.purchase.amount <= get_spend_cap(input.user.id)
}

# Deny purchases from blacklisted merchants
allow = false {
    input.purchase.merchant == "blacklisted_merchant"
}

# Deny purchases with certain MCC codes
allow = false {
    input.purchase.mcc == "illegal_mcc"
}

# Get spend cap for a user (in a real implementation, this would come from a database)
get_spend_cap(user_id) = cap {
    user_spend_caps[user_id] = cap
}

# Default spend cap
get_spend_cap(_) = 1000 {
    true
}

# User-specific spend caps
user_spend_caps = {
    "user1": 5000,
    "user2": 2000,
    "admin": 10000
}

# Merchant allowlist - only allow purchases from approved merchants
allow {
    merchant_allowlist[input.purchase.merchant]
}

# Approved merchants
merchant_allowlist = {
    "approved_merchant_1",
    "approved_merchant_2",
    "trusted_retailer"
}

# KYC check - require KYC for high-value transactions
allow = false {
    input.purchase.amount > 1000
    not user_kyc_verified[input.user.id]
}

# KYC status for users
user_kyc_verified = {
    "user1": true,
    "user2": true,
    "new_user": false
}