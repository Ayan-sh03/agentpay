package payments

# Default decision is to deny
default allow = false

# Main policy result
allow := count(deny_reasons) == 0

# Default explanation
default explanation = ["Purchase denied by default policy"]

# Explanation for allowed purchases
explanation = reasons {
    allowed := count(deny_reasons) == 0
    allowed == true
    reasons := ["Purchase approved by policy"]
}

# Explanation for denied purchases
explanation = reasons {
    count(deny_reasons) > 0
    reasons := deny_reasons
}

# Collect all deny reasons
deny_reasons[reason] {
    reason = check_spend_cap
    reason != ""
}

deny_reasons[reason] {
    reason = check_merchant_allowlist
    reason != ""
}

deny_reasons[reason] {
    reason = check_mcc
    reason != ""
}

deny_reasons[reason] {
    reason = check_kyc
    reason != ""
}

# Check spend cap
check_spend_cap := reason {
    input.purchase.amount > get_spend_cap(input.user.id)
    reason := sprintf("Purchase amount %v exceeds user spend cap of %v", [input.purchase.amount, get_spend_cap(input.user.id)])
} else := "" {
    input.purchase.amount <= get_spend_cap(input.user.id)
}

# Check merchant allowlist
check_merchant_allowlist := reason {
    not merchant_allowlist[input.purchase.merchant]
    reason := sprintf("Merchant '%v' is not in the allowlist", [input.purchase.merchant])
} else := "" {
    merchant_allowlist[input.purchase.merchant]
}

# Check MCC codes
check_mcc := reason {
    input.purchase.mcc == "illegal_mcc"
    reason := "Purchase has forbidden MCC code"
} else := "" {
    input.purchase.mcc != "illegal_mcc"
}

# KYC check - require KYC for high-value transactions
check_kyc := reason {
    input.purchase.amount > 1000
    not user_kyc_verified[input.user.id]
    reason := "KYC verification required for high-value transactions"
} else := "" {
    input.purchase.amount <= 1000
} else := "" {
    user_kyc_verified[input.user.id]
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
merchant_allowlist = {
    "approved_merchant_1",
    "approved_merchant_2",
    "trusted_retailer"
}

# KYC status for users
user_kyc_verified = {
    "user1": true,
    "user2": true,
    "new_user": false
}