package payments

# Default decision is to deny
default allow = false

# Allow purchases under $1000
allow {
    input.purchase.amount <= 1000
}

# Deny purchases from blacklisted merchants
allow = false {
    input.purchase.merchant == "blacklisted_merchant"
}

# Deny purchases with certain MCC codes
allow = false {
    input.purchase.mcc == "illegal_mcc"
}