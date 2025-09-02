package payments

# Default decision is to deny for safety
default allow := false

# Main policy result - agent is allowed if no deny reasons exist
allow if {
    count(deny_reasons) == 0
}

# Default explanation
default explanation := ["Purchase denied by default agent policy"]

# Explanation for allowed purchases
explanation := ["Purchase approved for agent by policy"] if {
    count(deny_reasons) == 0
}

# Explanation for denied purchases  
explanation := deny_reasons if {
    count(deny_reasons) > 0
}

# Collect all deny reasons for agents
deny_reasons contains reason if {
    reason := check_agent_spend_limit
    reason != ""
}

deny_reasons contains reason if {
    reason := check_digital_goods_only
    reason != ""
}

deny_reasons contains reason if {
    reason := check_agent_capabilities
    reason != ""
}

deny_reasons contains reason if {
    reason := check_time_restrictions
    reason != ""
}

# Check agent spending limits
check_agent_spend_limit := sprintf("Purchase amount %v exceeds agent limit of %v", [input.purchase.amount, get_agent_limit(input.user.id)]) if {
    input.purchase.amount > get_agent_limit(input.user.id)
} else := ""

# Digital goods only policy for agents  
check_digital_goods_only := sprintf("Agent can only purchase from digital goods merchants, '%v' is not allowed", [input.purchase.merchant]) if {
    not is_digital_goods_merchant(input.purchase.merchant)
} else := ""

# Check agent capabilities
check_agent_capabilities := sprintf("Agent lacks required capability for merchant '%v'", [input.purchase.merchant]) if {
    not agent_has_capability(input.user.id, required_capability_for_merchant(input.purchase.merchant))
} else := ""

# Time-based restrictions for agents (e.g., no purchases during maintenance hours)
check_time_restrictions := "Agent purchases restricted during maintenance window (2-4 AM UTC)" if {
    hour := time.clock(time.now_ns())[0]
    hour >= 2  # No purchases between 2-4 AM UTC (maintenance window)
    hour < 4
} else := ""

# Get spending limit for agent with default fallback
get_agent_limit(agent_id) := limit if {
    limit := agent_limits[agent_id]
} else := 500

# Agent-specific spending limits
agent_limits := {
    "demo-agent-001": 1000,
    "openai-agent-123": 2000,
    "claude-agent-456": 1500,
    "production-agent-789": 5000
}

# Digital goods merchants (courses, templates, designs, API credits)
digital_goods_merchants := {
    "udemy",
    "coursera", 
    "envato_market",
    "creative_market",
    "openai_api",
    "anthropic_api",
    "replicate_api",
    "figma_templates",
    "notion_templates"
}

# Helper function to check if merchant sells digital goods
is_digital_goods_merchant(merchant) if {
    digital_goods_merchants[merchant]
}

# Agent capabilities mapping
agent_capabilities := {
    "demo-agent-001": ["digital_goods", "api_calls"],
    "openai-agent-123": ["digital_goods", "api_calls", "subscriptions"], 
    "claude-agent-456": ["digital_goods"]
}

# Helper function to check agent capabilities
agent_has_capability(agent_id, capability) if {
    agent_capabilities[agent_id][_] == capability
}

# Required capability for each merchant
required_capability_for_merchant("openai_api") := "api_calls"
required_capability_for_merchant("anthropic_api") := "api_calls"
required_capability_for_merchant(_) := "digital_goods"