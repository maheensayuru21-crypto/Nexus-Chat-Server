# 🌐 Nexus Chat Server

A high-performance, multi-threaded backend communication server built in Java. This project serves as the real-time networking node for the **Nexus Ecosystem**, allowing concurrent users to communicate and securely interact with the Nexus Banking database via TCP/IP sockets.

## 🚀 Key Features
* **Multi-threaded Client Handling:** Manages multiple concurrent user connections without blocking the main server thread.
* **Stateful Identity Management:** Tracks active user sessions and dynamically routes broadcasts and private alerts.
* **Microservice Database Integration:** Connects seamlessly to the independent Nexus Banking MySQL database via JDBC.
* **ACID-Compliant Transactions:** Executes secure, multi-step financial transfers between users directly through the chat interface, ensuring complete data integrity.

## 🛠️ Tech Stack
* **Language:** Java (JDK 21)
* **Networking:** TCP/IP `java.net.Socket` and `ServerSocket`
* **Concurrency:** `Runnable` interfaces, Thread manipulation, `CopyOnWriteArrayList`
* **Database:** MySQL, JDBC (`mysql-connector-j`)

## 💻 Available Chat Commands
The server features a built-in command router that intercepts and processes specific user requests:
* `/users` - Retrieves a real-time list of all currently active clients in the chat.
* `/balance` - Queries the Nexus Banking database to display the user's current secure account balance.
* `/transfer [amount] [recipient]` - Initiates a secure SQL transaction to move funds between active accounts, instantly alerting the recipient upon success.

## ⚙️ Setup and Installation
1. Clone the repository: `git clone https://github.com/maheensayuru21-crypto/Nexus-Chat-Server.git`
2. Ensure your local MySQL server is running and the Nexus `bank_db` schema is initialized.
3. Update the `DatabaseManager.java` file with your specific MySQL credentials.
4. Compile and run `ChatServer.java` to start the listener on port `8080`.
5. Run multiple instances of `ChatClient.java` to simulate the network!

---
*Part of the Nexus Software Ecosystem.*