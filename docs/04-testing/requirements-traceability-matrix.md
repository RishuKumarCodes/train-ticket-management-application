# Requirements Traceability Matrix (RTM)
## RailFlow — Train Ticket Management Application
**Document Standard:** NASA SWE-052 / IEEE Bidirectional Traceability  
**Document Status:** Living Traceability Matrix (Updated as modules are implemented)

---

## 1. Overview
The Requirements Traceability Matrix (RTM) establishes bidirectional traceability between user requirements defined in the [SRS Document](../01-feasibility-and-requirements/srs.md), the system implementation packages, and the verification test cases in the [Test Plan](test-plan.md).

As each feature and test is implemented, its verification row will be populated here.

---

## 2. Traceability Matrix

| Requirement ID | Requirement Summary | Implementation Package / Class | Test Case ID | Test Status |
|---|---|---|---|---|
| **REQ-AUTH-01** | User registration & validation | `com.trainticket.model.service.AuthService`<br>`com.trainticket.model.dao.UserDAO`<br>`com.trainticket.view.dialog.AuthDialog` | `TC-AUTH-01` | **Passed** |
| **REQ-AUTH-02** | User authentication & role access | `com.trainticket.model.service.AuthService`<br>`com.trainticket.controller.AuthController`<br>`com.trainticket.view.dialog.AdminLoginDialog`<br>`com.trainticket.view.admin.AdminDashboardFrame` | `TC-AUTH-02` | **Passed** |
| **REQ-TRN-01** | Multi-criteria train search interface | `com.trainticket.view.pages.HomeView` | `TC-TRN-01` | In Progress |
| **REQ-TRN-02** | Schedules, stops & coach fare view | *(Mapped upon implementation)* | `TC-TRN-02` | Planned |
| **REQ-SEAT-01** | Interactive coach seat map | *(Mapped upon implementation)* | `TC-SEAT-01` | Planned |
| **REQ-BKG-01** | Unique PNR generation & booking | *(Mapped upon implementation)* | `TC-BKG-01` | Planned |
| **REQ-BKG-02** | Atomic reservation transaction | *(Mapped upon implementation)* | `TC-BKG-02` | Planned |
| **REQ-CNL-01** | Ticket cancellation & refund logic | *(Mapped upon implementation)* | `TC-CNL-01` | Planned |
| **REQ-ADM-01** | Admin train & route configuration | *(Mapped upon implementation)* | `TC-ADM-01` | Planned |
| **NFR-PERF-01** | Low-latency DB queries via HikariCP | *(Mapped upon implementation)* | `TC-PERF-01` | Planned |
| **NFR-REL-01** | Zero EDT freezing during background I/O | *(Mapped upon implementation)* | `TC-REL-01` | Planned |
