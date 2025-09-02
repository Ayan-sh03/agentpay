# 🤖 **AI Agent Payment Gateway - Demo Usage**

## **Quick Demo Script**

### **1. Start the Application**
```bash
# Start OPA server (in separate terminal)
./opa.exe run -s

# Load agent policies
curl -X PUT http://localhost:8181/v1/policies/payments --data-binary @example_policy.rego

# Start payment gateway
./mvnw spring-boot:run
```

### **2. Agent Authentication (Get JWT Token)**
```bash
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "demo-key"}'

# Response:
{
  "accessToken": "eyJ0eXAi...",
  "tokenType": "Bearer", 
  "expiresIn": 3600
}
```

### **3. Agent Purchase - SUCCESS Case**
```bash
curl -X POST http://localhost:8080/api/v1/purchase \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50,
    "merchant": "udemy",
    "productType": "course",
    "productId": "advanced-python-course",
    "currency": "USD",
    "description": "Python course for my development"
  }'

# Expected Response:
{
  "transactionId": "uuid-here",
  "status": "APPROVED", 
  "message": "Purchase approved by policy and ready for payment processing."
}
```

### **4. Agent Purchase - DENIAL Case**
```bash
curl -X POST http://localhost:8080/api/v1/purchase \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 2000,
    "merchant": "physical_store",
    "productType": "hardware",
    "productId": "laptop-123", 
    "currency": "USD"
  }'

# Expected Response:
{
  "transactionId": "uuid-here",
  "status": "DENIED",
  "message": "Purchase denied by policy. Owner approval may be possible."
}
```

## **What Makes This Agent-Centric:**

### **🚫 What We REMOVED (Human Stuff):**
- WebAuthn/FIDO2 biometric authentication
- OAuth2 browser-based flows
- Spring Cloud Gateway complexity
- Human-centric validation (KYC, etc.)

### **✅ What We ADDED (Agent Stuff):**
- API key authentication (perfect for bots)
- Digital goods focus (courses, templates, API credits)
- Agent capabilities system ("digital_goods", "api_calls") 
- Owner/developer relationship (who controls the agent)
- Programmatic approval workflows
- Agent-specific spending limits

### **💡 Real Agent Use Cases:**
- **OpenAI Agent** buying API credits from OpenAI
- **Design Agent** purchasing templates from Envato Market
- **Learning Agent** buying courses from Udemy/Coursera
- **Research Agent** subscribing to data APIs

### **🔒 Security That Actually Works:**
- Hashed API keys (not hardcoded users)
- AES-GCM encryption (not broken ECB mode)
- JWT with agent-specific claims
- OPA policies that actually deny bad requests

## **Demo Talking Points:**

1. **"This isn't just another Stripe wrapper"** - It's designed for AI agents, not humans
2. **"Policy enforcement works"** - Show the OPA denial explanations
3. **"Real security"** - AES-GCM encryption, proper JWT, hashed keys
4. **"Agent-native"** - API keys, digital goods focus, capability system
5. **"Scalable foundation"** - Spring Boot reactive, OPA external policies, audit trails

**This system is ready for real agent workloads!**