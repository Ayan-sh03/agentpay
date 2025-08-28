package payments

# Default decision is to deny
default allow = false

# Main policy result
allow if count(deny_reasons) == 0

# Default explanation
default explanation = ["Purchase denied by default policy"]

# Explanation for allowed purchases
explanation = ["Purchase approved by policy"] if count(deny_reasons) == 0

# Explanation for denied purchases
explanation = deny_reasons if count(deny_reasons) > 0

# Collect all deny reasons
deny_reasons[reason] if {
    reason := check_spend_cap
    reason != ""
}

deny_reasons[reason] if {
    reason := check_merchant_allowlist
    reason != ""
}

deny_reasons[reason] if {
    reason := check_mcc
    reason != ""
}

deny_reasons[reason] if {
    reason := check_kyc
    reason != ""
}

# Check spend cap
check_spend_cap := sprintf("Purchase amount %v exceeds user spend cap of %v", [input.purchase.amount, get_spend_cap(input.user.id)]) if input.purchase.amount > get_spend_cap(input.user.id)
check_spend_cap := "" if input.purchase.amount <= get_spend_cap(input.user.id)

# Check merchant allowlist
check_merchant_allowlist := sprintf("Merchant '%v' is not in the allowlist", [input.purchase.merchant]) if not merchant_allowlist[input.purchase.merchant]
check_merchant_allowlist := "" if merchant_allowlist[input.purchase.merchant]

# Check MCC codes
check_mcc := "Purchase has forbidden MCC code" if input.purchase.mcc == "illegal_mcc"
check_mcc := "" if input.purchase.mcc != "illegal_mcc"

# KYC check - require KYC for high-value transactions
check_kyc := "KYC verification required for high-value transactions" if {
    input.purchase.amount > 1000
    not user_kyc_verified[input.user.id]
}
check_kyc := "" if input.purchase.amount <= 1000
check_kyc := "" if user_kyc_verified[input.user.id]

# Get spend cap for a user (in a real implementation, this would come from a database)
get_spend_cap(user_id) := user_spend_caps[user_id]

# Default spend cap - only applies when user is not in the user_spend_caps map
get_spend_cap(user_id) := 1000 if not user_spend_caps[user_id]

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