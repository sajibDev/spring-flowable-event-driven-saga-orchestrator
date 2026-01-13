# Flowable UI Screenshots Guide

## 🎯 What You'll See in Flowable Admin UI

This document describes what each screen in the Flowable Admin UI looks like and what information you'll find.

## 1. Login Screen

```
┌─────────────────────────────────────────────────┐
│        FLOWABLE ADMIN                            │
│                                                  │
│   Username: [admin              ]               │
│   Password: [••••••             ]               │
│                                                  │
│              [ Log In ]                          │
│                                                  │
└─────────────────────────────────────────────────┘
```

**Credentials**: admin / admin

## 2. Main Dashboard

```
┌────────────────────────────────────────────────────────────┐
│ FLOWABLE ADMIN                                    [Logout] │
├────────────────────────────────────────────────────────────┤
│                                                            │
│ ┌─────────────────┐  ┌─────────────────┐                 │
│ │ Process Engine  │  │   CMMN Engine   │                 │
│ │                 │  │                 │                 │
│ │ • Definitions   │  │ • Definitions   │                 │
│ │ • Instances     │  │ • Instances     │                 │
│ │ • Jobs          │  │ • Jobs          │                 │
│ │ • History       │  │                 │                 │
│ └─────────────────┘  └─────────────────┘                 │
│                                                            │
│ ┌─────────────────┐  ┌─────────────────┐                 │
│ │  Form Engine    │  │   DMN Engine    │                 │
│ │                 │  │                 │                 │
│ │ • Definitions   │  │ • Definitions   │                 │
│ │ • Instances     │  │ • Instances     │                 │
│ └─────────────────┘  └─────────────────┘                 │
└────────────────────────────────────────────────────────────┘
```

**Click on**: "Process Engine" to access saga monitoring

## 3. Process Engine - Definitions

```
┌────────────────────────────────────────────────────────────┐
│ Process Engine > Definitions > Process Definitions         │
├────────────────────────────────────────────────────────────┤
│                                                            │
│ [Search: ____________]                     [Refresh]       │
│                                                            │
│ Key              │ Name               │ Ver │ Deployed    │
│ ──────────────────────────────────────────────────────────│
│ orderSagaProcess │ Order Saga Process │ 1   │ 2025-11-29  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**Actions**:
- Click on "orderSagaProcess" to view BPMN diagram
- See all deployed process versions

## 4. Process Engine - Process Instances

```
┌───────────────────────────────────────────────────────────────────────┐
│ Process Engine > Instances > Process Instances                        │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│ [Business Key: ____________]  [State: ▼ All]  [Search]  [Refresh]    │
│                                                                       │
│ Business Key         │ Process Def      │ State    │ Started          │
│ ─────────────────────────────────────────────────────────────────────│
│ a1b2c3d4-e5f6-...   │ orderSagaProcess │ Running  │ 2025-11-29 14:30│
│ f7g8h9i0-j1k2-...   │ orderSagaProcess │ Completed│ 2025-11-29 14:28│
│ m3n4o5p6-q7r8-...   │ orderSagaProcess │ Completed│ 2025-11-29 14:25│
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

**Tips**:
- Business Key = your orderId
- Click on any row to open details
- Green "Running" = still processing
- Blue "Completed" = finished

## 5. Process Instance Details - Diagram Tab

```
┌────────────────────────────────────────────────────────────────────┐
│ Process Instance: a1b2c3d4-e5f6-...                                │
│ Business Key: a1b2c3d4-e5f6-... | State: Running                   │
├────────────────────────────────────────────────────────────────────┤
│ [ Diagram ] [ Variables ] [ Activities ] [ Jobs ]                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│    ┌───────┐     ┌──────────────┐     ┌──────────────┐           │
│    │ Start │────→│ Create Order │────→│Reserve Inven │           │
│    └───────┘     └──────────────┘     └──────────────┘           │
│                                              │                     │
│                                              ▼                     │
│                                      ┌───────────────┐            │
│                                      │ Wait for Inv  │ 🟢         │
│                                      │   Response    │            │
│                                      └───────────────┘            │
│                                         /          \              │
│                                       ✅           ❌             │
│                                      /              \             │
│                       ┌──────────────┐    ┌──────────────┐       │
│                       │ Inv Success  │    │ Inv Failure  │       │
│                       └──────────────┘    └──────────────┘       │
│                             │                     │               │
│                             ▼                     ▼               │
│                    ┌──────────────┐      ┌──────────────┐        │
│                    │Process Payment│      │Cancel Order │        │
│                    └──────────────┘      └──────────────┘        │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**Legend**:
- 🟢 Green highlight = Current active step
- Gray elements = Not yet executed
- Completed elements = Already done

## 6. Process Instance Details - Variables Tab

```
┌────────────────────────────────────────────────────────────────────┐
│ Process Instance: a1b2c3d4-e5f6-...                                │
│ Business Key: a1b2c3d4-e5f6-... | State: Running                   │
├────────────────────────────────────────────────────────────────────┤
│ [ Diagram ] [ Variables ] [ Activities ] [ Jobs ]                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ Name              │ Type    │ Value                                │
│ ──────────────────────────────────────────────────────────────────│
│ orderId           │ String  │ a1b2c3d4-e5f6-7890-1234-567890abcdef│
│ customerId        │ String  │ CUST-001                             │
│ totalAmount       │ String  │ 1200.00                              │
│ shippingAddress   │ String  │ 123 Main St, New York, NY 10001      │
│ orderData         │ Object  │ {"orderId":"a1b2c3d4...","items":[]}│
│ reservationId     │ String  │ res-abc123-PROD-001                  │
│ transactionId     │ String  │ null                                 │
│ shipmentId        │ String  │ null                                 │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

**What to look for**:
- `orderId`: Your order identifier
- `reservationId`: Appears after inventory step
- `transactionId`: Appears after payment step
- `shipmentId`: Appears after shipping step
- `failureReason`: Shows why saga failed (if it did)

## 7. Process Instance Details - Activities Tab

```
┌────────────────────────────────────────────────────────────────────┐
│ Process Instance: a1b2c3d4-e5f6-...                                │
│ Business Key: a1b2c3d4-e5f6-... | State: Completed                 │
├────────────────────────────────────────────────────────────────────┤
│ [ Diagram ] [ Variables ] [ Activities ] [ Jobs ]                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ Activity             │ Type         │ Started     │ Duration       │
│ ──────────────────────────────────────────────────────────────────│
│ startEvent           │ startEvent   │ 14:30:00    │ 1ms            │
│ createOrderTask      │ serviceTask  │ 14:30:00    │ 245ms          │
│ reserveInventoryTask │ serviceTask  │ 14:30:01    │ 180ms          │
│ waitForInventory...  │ eventGateway │ 14:30:01    │ 523ms          │
│ inventoryReserved... │ catchEvent   │ 14:30:02    │ 12ms           │
│ processPaymentTask   │ serviceTask  │ 14:30:02    │ 312ms          │
│ waitForPayment...    │ eventGateway │ 14:30:02    │ 445ms          │
│ paymentProcessed...  │ catchEvent   │ 14:30:03    │ 8ms            │
│ createShipmentTask   │ serviceTask  │ 14:30:03    │ 156ms          │
│ waitForShipment...   │ eventGateway │ 14:30:03    │ 234ms          │
│ shipmentCreated...   │ catchEvent   │ 14:30:04    │ 5ms            │
│ endEventSuccess      │ endEvent     │ 14:30:04    │ 1ms            │
│                                                                     │
│ Total Duration: 4.2 seconds                                        │
└────────────────────────────────────────────────────────────────────┘
```

**Use this to**:
- See complete execution history
- Find performance bottlenecks
- Understand the flow sequence
- Debug timing issues

## 8. Failed Saga Example - Compensation Flow

```
┌────────────────────────────────────────────────────────────────────┐
│ Process Instance: m3n4o5p6-q7r8-...                                │
│ Business Key: m3n4o5p6-q7r8-... | State: Completed (Failed)        │
├────────────────────────────────────────────────────────────────────┤
│ [ Diagram ] [ Variables ] [ Activities ] [ Jobs ]                  │
├────────────────────────────────────────────────────────────────────┤
│ Variables Tab:                                                      │
│                                                                     │
│ failureReason     │ String  │ Payment gateway declined             │
│ reservationId     │ String  │ res-xyz789-PROD-001                  │
│ transactionId     │ String  │ null                                 │
│                                                                     │
├────────────────────────────────────────────────────────────────────┤
│ Activities Tab (showing compensation):                             │
│                                                                     │
│ Activity                  │ Type        │ Started  │ Duration      │
│ ──────────────────────────────────────────────────────────────────│
│ startEvent                │ startEvent  │ 14:35:00 │ 1ms           │
│ createOrderTask           │ serviceTask │ 14:35:00 │ 240ms         │
│ reserveInventoryTask      │ serviceTask │ 14:35:01 │ 175ms ✅      │
│ inventoryReservedSuccess  │ catchEvent  │ 14:35:02 │ 10ms          │
│ processPaymentTask        │ serviceTask │ 14:35:02 │ 290ms         │
│ paymentProcessedFailure   │ catchEvent  │ 14:35:03 │ 8ms  ❌       │
│ compensateInventoryTask   │ serviceTask │ 14:35:03 │ 120ms ⚠️      │
│ cancelOrderTask           │ serviceTask │ 14:35:03 │ 95ms          │
│ endEventFailure           │ endEvent    │ 14:35:04 │ 1ms           │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

**Notice**:
- ✅ Inventory succeeded
- ❌ Payment failed
- ⚠️ Compensation executed (released inventory)
- Order cancelled

## 9. Event Subscriptions Screen

```
┌────────────────────────────────────────────────────────────────────┐
│ Process Engine > Jobs > Event Subscriptions                        │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│ [Search: ____________]                     [Refresh]                │
│                                                                     │
│ Event Name              │ Process Inst │ Activity            │ Created│
│ ──────────────────────────────────────────────────────────────────│
│ inventoryReservedSuccess│ a1b2c3d4-... │ waitForInventory... │ 14:30 │
│ inventoryReservedFailure│ a1b2c3d4-... │ waitForInventory... │ 14:30 │
│ paymentProcessedSuccess │ f7g8h9i0-... │ waitForPayment...   │ 14:31 │
│ paymentProcessedFailure │ f7g8h9i0-... │ waitForPayment...   │ 14:31 │
│ shipmentCreatedSuccess  │ m3n4o5p6-... │ waitForShipment...  │ 14:32 │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

**This shows**:
- Which processes are waiting for events
- What event they're waiting for
- Where in the workflow they're stuck

## 10. Navigation Flow

```
Main Dashboard
    │
    ├─→ Process Engine
    │     │
    │     ├─→ Definitions
    │     │     └─→ Process Definitions
    │     │           └─→ orderSagaProcess (view BPMN)
    │     │
    │     ├─→ Instances
    │     │     ├─→ Process Instances
    │     │     │     └─→ Click instance
    │     │     │           ├─→ Diagram tab (visual flow)
    │     │     │           ├─→ Variables tab (data)
    │     │     │           ├─→ Activities tab (history)
    │     │     │           └─→ Jobs tab (async jobs)
    │     │     │
    │     │     └─→ Tasks (user tasks - not used in this saga)
    │     │
    │     ├─→ Jobs
    │     │     ├─→ Jobs (async jobs)
    │     │     ├─→ Failed Jobs (errors)
    │     │     └─→ Event Subscriptions (waiting events)
    │     │
    │     └─→ History
    │           └─→ Historic Process Instances
    │
    └─→ Other Engines (CMMN, DMN, Form - not used)
```

## 🎯 Quick Access Cheat Sheet

| **To View** | **Navigate To** | **What You'll Find** |
|------------|----------------|---------------------|
| All Orders | Process Engine → Instances → Process Instances | List of all sagas |
| Order Details | Click on instance row | Full details of specific order |
| Visual Flow | Instance → Diagram tab | BPMN with current position |
| Order Data | Instance → Variables tab | All process variables |
| Execution Log | Instance → Activities tab | Complete activity history |
| BPMN Definition | Definitions → Process Definitions → orderSagaProcess | Your BPMN diagram |
| Waiting Events | Jobs → Event Subscriptions | Active message subscriptions |
| Failed Jobs | Jobs → Failed Jobs | Jobs that errored |

## 🔍 Search Tips

### Find Order by ID
1. Go to Process Instances
2. Enter orderId in "Business Key" search box
3. Click Search

### Filter by State
1. Use "State" dropdown
2. Options: All, Running, Completed, Suspended
3. View only active or completed orders

### Time Range
1. Use date filters (if available)
2. Find orders from specific time period

---

**This UI gives you complete visibility into your saga orchestration!** 🎭
