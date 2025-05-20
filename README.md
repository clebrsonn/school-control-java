# 🎓 School Control

**School Control** is a Spring Boot-based application designed to manage tutoring school operations efficiently.  
It provides tools for registering students, guardians, users, generating invoices, tracking expenses, and applying automatic discounts when multiple students are linked to the same responsible party.

---

## 🚀 Technologies Used

- Java 23+
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Docker & Docker Compose
- JUnit 5
- Gradle

---

## 📦 Installation

### Requirements

- Java 23 or higher
- Docker & Docker Compose
- PostgreSQL

### Running with Docker

```bash
- docker-compose up --build
```

### Running Locally

```bash
- ./gradlew bootRun
```

### 🧪 Running Tests

```bash
- ./gradlew test
```

## 💡 Features

- User, student, and guardian management
- Link multiple students to a single guardian
- Automatic invoice generation (monthly tuition)
- Progressive discount system based on student count
- Expense tracking and cash flow control
- Admin dashboard (under development)

## 📬 Example API Endpoints

| Method | Route                | Description               |
| ------ |----------------------| ------------------------- |
| GET    | `/students`          | List all students         |
| POST   | `/responsibles`      | Register a new guardian   |
| POST   | `/invoices/generate` | Generate invoices         |
| GET    | `/expenses/monthly`  | Retrieve monthly expenses |

## ⚙️ Environment Variables

You can use a .env file or configure it via application.properties.

## 🐳 Docker

To build the Docker image using Spring Boot:

```bash
./gradlew bootBuildImage
```

## 🔐 License

This project is licensed under the terms of the [GNU GPL v3](LICENSE).

© 2025 Clebrsonn

🙋‍♂️ Author
Developed with 💻 and ☕ by Clebrsonn

[LinkedIn]() – [GitHub]()
