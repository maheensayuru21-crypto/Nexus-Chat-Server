# 🌐 Nexus Chat Server

A highly secure, high-performance client-server communication and banking platform featuring the **Nexus Security Gateway**. This project serves as the real-time networking node for the Nexus Ecosystem, allowing concurrent users to communicate and securely interact with the Nexus Banking database via encrypted TCP/IP sockets.

## 📸 Interface Previews

<p align="center">
<img width="652" height="482" alt="Screenshot 2026-05-02 092341" src="https://github.com/user-attachments/assets/6af4e7a0-407e-4f7b-8a29-a396bc689360" />
<img width="651" height="482" alt="Screenshot 2026-05-02 092502" src="https://github.com/user-attachments/assets/a5d577d5-2457-4fb8-81e9-68eace6d42c4" />

</p>

## 🚀 Key Features
* **End-to-End Encryption:** All socket communication between the client and server is secured using SSL/TLS with custom keystore certificates.
* **Cryptographic Identity Management:** User passwords are encrypted using the SHA-256 hash function before being stored in the database.
* **Multi-threaded Client Handling:** Manages multiple concurrent user connections without blocking the main server thread.
* **Modern GUI:** A polished, responsive desktop client built with JavaFX, featuring dynamic text routing and a dark-theme aesthetic.
* **Offline Message Queuing:** Direct messages sent to offline users are safely queued in the database and delivered immediately upon their next login.
* **ACID-Compliant Transactions:** Executes secure, multi-step financial transfers between users directly through the chat interface, ensuring complete data integrity.

## 🛠️ Tech Stack
* **Language:** Java (JDK 21)
* **GUI Framework:** JavaFX
* **Build Tool:** Maven (Configured for Fat JAR executables)
* **Database:** MySQL 8.0+ & JDBC (`mysql-connector-j`)
* **Security:** Java Secure Socket Extension (JSSE), `java.security.MessageDigest`
* **Networking:** TCP/IP `java.net.Socket` and `ServerSocket`

## 💻 Available Chat Commands
The server features a built-in command router that intercepts and processes specific user requests:
* `/users` - Retrieves a real-time list of all currently active clients in the chat.
* `/msg [username] [message]` - Send a direct message (safely queues if the recipient is offline).
* `/balance` - Queries the Nexus Banking database to display your current secure account balance.
* `/transfer [amount] [recipient]` - Initiates a secure SQL transaction to move funds, instantly alerting the recipient.
* `/history` - View your recent transaction ledger.

*Admin Commands:*
* `/freeze [username]` - Lock a malicious account from making transactions.
* `/reset [username]` - Wipe an account's balance to zero.

## ⚙️ Setup and Installation

### Prerequisites
1. Ensure you have **Java 21** or higher installed.
2. Ensure you have **MySQL 8.0+** installed and running on `localhost:3306`.

### Database Setup
1. Open MySQL Workbench and create the database: `CREATE DATABASE bank_db;`
2. Execute your SQL schema to generate the `accounts`, `transactions`, and `offline_messages` tables.

### Running the Application (Executable JARs)
The project is packaged into standalone executables via Maven. **Note:** Ensure the `nexus_keystore.p12` file is located in the exact same directory as your `.jar` files.

**1. Start the Server**
Open a terminal in the target directory and run:
```bash
java -jar NexusServer.jar
```
*The terminal will confirm when the SSL Server is active and listening on port 5000.*

**2. Launch the Client**
Open a separate terminal in the target directory and run:
```bash
java -jar NexusClient.jar
```

---
*Developed by M4H33N. Part of the Nexus Software Ecosystem.*
