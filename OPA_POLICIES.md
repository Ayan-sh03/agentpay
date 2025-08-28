# Open Policy Agent (OPA) Policies

This document explains how to set up and use OPA policies with the Payment Agent.

## Prerequisites

- Install OPA: https://www.openpolicyagent.org/docs/latest/#running-opa

## Loading Policies

1. Start OPA server:
   ```bash
   opa run -s
   ```

2. Load the policy file:
   ```bash
   curl -X PUT http://localhost:8181/v1/policies/payments --data-binary @example_policy.rego
   ```

## Testing Policies

You can test policies directly with OPA:

```bash
curl -X POST http://localhost:8181/v1/data/payments/allow \
  -H "Content-Type: application/json" \
  -d '{
    "input": {
      "purchase": {
        "amount": 500,
        "merchant": "valid_merchant",
        "mcc": "valid_mcc"
      }
    }
  }'
```

## Policy Structure

The current example policy evaluates purchases based on:
- Amount limits (currently $1000)
- Blacklisted merchants
- Restricted MCC codes

Modify the policy according to your business requirements.

## Integration with Payment Agent

The Payment Agent sends purchase requests to OPA for evaluation. The structure sent to OPA is:

```json
{
  "input": {
    "purchase": {
      "userId": "user123",
      "agentId": "agent456",
      "amount": 100.0,
      "merchant": "some_merchant",
      "mcc": "some_mcc"
    }
  }
}
```

OPA responds with a boolean allow/deny decision that the Payment Agent uses to determine whether to proceed with the purchase.