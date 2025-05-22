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

## Project Architecture

The project follows a standard layered architecture:

- **Controllers:** Handle incoming HTTP requests, validate input, and delegate to service layer.
- **Services:** Contain business logic, orchestrate interactions between repositories and other services.
- **Repositories:** Interact with the database, providing an abstraction layer over data access.
- **Domain Models:** Represent the core entities of the application (e.g., Student, Guardian, Invoice).

These layers interact sequentially, with controllers calling services, services using repositories, and repositories managing domain models.

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

## 🤝 Contributing

We welcome contributions to enhance School Control! To contribute:

1. **Fork the repository.**
2. **Create a new branch** for your feature or bug fix: `git checkout -b feature/your-feature-name` or `git checkout -b fix/your-bug-fix`.
3. **Make your changes.** Ensure your code adheres to existing coding standards.
4. **Write tests** for your changes, if applicable.
5. **Commit your changes:** `git commit -m "Add your commit message here"`.
6. **Push to your branch:** `git push origin feature/your-feature-name`.
7. **Submit a pull request** for review.

## 🔍 Troubleshooting

Here are some common issues and their solutions:

- **Database Connection Issues:**
    - Ensure PostgreSQL is running.
    - Verify that the database credentials in `application.properties` (or your `.env` file) are correct.
    - Check Docker container logs if running with Docker Compose.
- **Build Failures:**
    - Ensure you have Java 23+ installed and configured correctly.
    - Clean the build: `./gradlew clean`.
    - Rebuild the project: `./gradlew build`.
- **`./gradlew bootRun` fails:**
    - Ensure all dependencies are correctly downloaded. Try running `./gradlew dependencies` to check.
    - Make sure the PostgreSQL database is running and accessible.

## 🔐 License

This project is licensed under the terms of the [GNU GPL v3](LICENSE).

© 2025 Clebrsonn

🙋‍♂️ Author
Developed with 💻 and ☕ by Clebrsonn

[LinkedIn]() – [GitHub]()
