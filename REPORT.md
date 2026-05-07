# A20 NotIST Project Report

### Index
1. [Introduction](#1-introduction)
2. [Project Development](#2-project-development)
   1. [Secure Document Format](#21-secure-document-format)
   2. [Infrastructure](#22-infrastructure)
   3. [Security Challenge](#23-security-challenge)
3. [Conclusion](#3-conclusion)
4. [Bibliography](#4-bibliography)

## 1. Introduction

**NotIST** is an application that allows users to create personal and shared notes. It's an **app** that stores every note on the server, and doesn't leave local saves. The app prioritizes **privacy**, ensuring notes are encrypted and accessible only to authorized users.

To achieve this, we developed:
1. A **secure document format** for encrypting and protecting notes (provided as a library).
2. A **virtual infrastructure** with secure servers and an API to manage notes.

### API Features
The API supports the following operations:
- **signup**
- **login**
- **logout**
- **nnote: Creates a note**
- **mnotes: Print personal notes**
- **snotes: Print notes user has access to (and the roles)** (roles: owner, editor, or viewer)
- **rnote: Read note** (based on access level)
- **enote: Edit note** (including content, viewers, and editors if owner of the note)

This project addresses **security challenge A**: enabling secure sharing of notes. Shared notes must remain confidential and tamper-proof, with access restricted to authorized users.

---

## 2. Project Development

### 2.1. Secure Document Format

#### 2.1.1. Design

The secure document format ensures **confidentiality**, **integrity**, and **authenticity** of JSON documents. **AES-GCM** was chosen for its ability to combine encryption and integrity verification.

**Structure of Encrypted Note**:
- **iv**: Base64-encoded Initialization Vector (randomly generated).
- **note**: Base64-encoded encrypted content.
- **hmac**: Base64-encoded HMAC for integrity verification.

Unencrypted fields (e.g., title, users) enable **identification** and **management** without compromising confidentiality. This format guarantees robust cryptographic protection while maintaining simplicity for storage and transmission.

**Example Encrypted Note**:
```json
{
    "id": 123,
    "title": "Example Document",
    "note": "RoO2DEzXAXgi+wauuE2CTNsxLSLSm9rbno5nq2NbXeG12HxI1EUqWavl8N0=",
    "date_created": "2022-01-01T12:00:00Z",
    "date_modified": "2022-01-02T12:00:00Z",
    "last_modified_by": 456,
    "version": 3,
    "owner": {
        "id": 456,
        "username": "john"
    },
    "editors": [
        { "id": 789, "username": "jane" },
        { "id": 1011, "username": "bob" }
    ],
    "viewers": [
        { "id": 1213, "username": "alice" },
        { "id": 1415, "username": "charlie" }
    ],
    "iv": "j+tzNrxos2gRQ6N7",
    "hmac": "TRQelr0R7t8V6NWek/nnaoBNfqbF572qSE+9xTYjxqM="
}
```

### 2.1.2. Implementation

#### Tools and Libraries

- **Language**: Java  
- **Encryption**: `javax.crypto` (AES-GCM)  
- **HMAC**: `Mac` class  
- **JSON Handling**: Google Gson  
- **Sending keys to server**: RSA Encryptation

---

#### Encryption Workflow

1. **Load Inputs**  
    - Plaintext JSON file  
    - AES Key file  
    - HMAC Key file  

2. **AES Encryption**  
    - Extract "note" from JSON  
    - Load AES key and generate IV  
    - Encrypt "note" using AES-GCM  
    - Base64 encode encrypted "note" and IV  
    - Add encrypted "note" and IV to JSON  

3. **Compute HMAC**  
    - Load HMAC key  
    - Serialize JSON (excluding HMAC field)  
    - Compute HMAC and Base64 encode  
    - Add HMAC to JSON  

4. **Save Encrypted JSON**  
    - Serialize JSON with encrypted "note", IV, and HMAC  
    - Save to output file  

---

#### Decryption Workflow

1. **Load Inputs**  
    - Encrypted JSON file  
    - AES key and HMAC key files  

2. **Verify HMAC**  
    - Extract and remove HMAC from JSON  
    - Compute HMAC of remaining JSON  
    - Compare with extracted HMAC  

3. **AES Decryption**  
    - Decode IV and ciphertext  
    - Decrypt using AES-GCM  
    - Restore plaintext "note"  

4. **Save Plaintext JSON**  
    - Serialize and save restored JSON  

---

#### Validation Workflow

1. **Verify Fields**  
    - Ensure JSON contains required fields (`iv`, `note`, `hmac`)  
    - Check Base64 encoding  

2. **Verify HMAC**  
    - Recompute HMAC and compare with stored value  

---

#### Sending the keys to/from the Server

##### To the server

1. **Get the keys from their files**
2. **Encrypt them with the user private key**
3. **Send them alongside the note via grpc to the server**

##### From the server

1. **Get the keys from the database tables**
2. **Decrypt them with the public key of the last user to modify the note**
3. **Encrypt it with the public key of the user asking to read/edit the note**
4. **Send them alongside the note via grpc to the server**

---

#### Challenges Faced

1. **Environment Setup**  
    - First, setting up the project environment was difficult because the Gson library used in Lab 4 could not be successfully downloaded and configured on our local machines. The Gson library likely failed due to Maven dependency configuration issues, network restrictions, or environment-specific differences. To resolve this, we used the virtual machine (VM) where the secure writer and reader were implemented, and we copied the pom.xml and environment configuration from there. 

2. **Key Management**  
    - Second, managing keys for the encryption and HMAC processes was challenging. We wanted to encrypt and sign with 2 different keys to ensure separation of concerns, enhances security, and adheres to cryptographic best practices.To address this, we assumed each user would have a private secret key for encryption, similar to Lab 4. We also created a KeyGenerator tool to generate private keys. Like that we could create one used for the HMAC signature.Configuration issues with Gson library were resolved by using a virtual machine with proper Maven setup.  

---

### 2.2. Infrastructure

#### 2.2.1. Network and Machine Setup

#### Infrastructure Overview

The infrastructure consists of five virtual machines (VMs) designed to ensure:

- **Secure Communication**
- **Encrypted Data Storage**
- **Strict Traffic Control**

#### VM1: Client Machine

The client machine initiates requests to the note-taking service hosted on the server (VM3) using **gRPC**.

- **IP Address:** `192.168.0.100` (part of the `192.168.0.0/24` subnet)

---

#### VM3: Server Machine

The server machine acts as the central hub for the application, handling client requests, interacting with the database, and ensuring secure access control.

- **Communication Protocol:** gRPC with **Transport Layer Security (TLS)** for encrypted and authenticated communication.
- **IP Address:** `192.168.1.1` (part of the `192.168.1.0/24` subnet)

---

#### VM4: Database Machine

The database machine securely stores all application data, including notes. Data confidentiality is ensured through encryption and restricted access.

##### Security Features:

- **Encryption:** Data is stored in an encrypted format, making it unreadable without proper keys.
- **Authentication:** Only the server (VM3) can access the database through authenticated and encrypted connections.

- **IP Address:** `192.168.2.4` (part of the `192.168.2.0/24` subnet)

---

#### VM2: Central Firewall

The central firewall mediates traffic between the client (VM1) and the server (VM3), enforcing strict traffic rules.

#### Configuration:

- **Interface eth0 (to VM1):** `192.168.0.1`
- **Interface eth1 (to VM3):** `192.168.1.254`

#### Traffic Rules:

- Allow only **gRPC traffic** (TCP port `50052`) between VM1 and VM3.
- Block all unauthorized or malicious traffic.

---

#### VM5: Database Firewall

The database firewall provides an additional security layer between the server (VM3) and the database (VM4), enforcing the principle of least privilege.

##### Configuration:

- **Interface eth0 (to VM3):** `192.168.1.1`
- **Interface eth1 (to VM4):** `192.168.2.4`

##### Traffic Rules:

- Allow only **database traffic** (TCP port `5432`) from VM3 to VM4 and vice-versa.

This segmentation enhances security by isolating the database from direct exposure, ensuring only approved connections are allowed.

---

#### Infrastructure Diagram

![Infrastructure Schema](img/infrastructure_schema.png)

---

### Justification of Technology Choices

#### gRPC Framework

- High-performance communication.
- Efficient data serialization with **Protocol Buffers**.
- Native integration of encryption and authentication mechanisms.

#### TLS for Encryption

- Ensures confidentiality by encrypting all data in transit.
- Enables authentication via certificates, protecting against unauthorized access or data interception.

#### Firewalls (VM2 and VM5)

##### Central Firewall (VM2):

- Controls communication between the client and server.
- Restricts traffic to **gRPC over TCP port 50052**, minimizing the attack surface.

##### Database Firewall (VM5):

- Adds a protective layer for the database.
- Isolates the database from direct access.
- Allows only authenticated and encrypted communication on **TCP port 5432**.

---

#### 2.2.2. Server Communication Security

To ensure secure communication between the client and the server, the following measures were implemented:

- **Transport Layer Security (TLS):** All communications between the client (VM1) and the server (VM3) are encrypted using TLS. This ensures the confidentiality and integrity of data in transit.
- **Strict Protocols:** Only gRPC traffic over a specific port (e.g., TCP port 50052) is permitted, minimizing exposure to unauthorized access.

### 2.3. Security Challenge

#### 2.3.1. Challenge Overview

The security challenge introduced new requirements for enhancing the confidentiality and integrity of shared notes. We have to implement a system to manage user roles (e.g., owner, editor, viewer) for each note, ensuring only authorized users can access or modify the content.

Summary of Security Requirements

**SR1 (Confidentiality)**: AES-GCM ensures confidentiality by encrypting the note's content, making it accessible only to the owner or authorized users.

**SR2 (Integrity 1)**: HMAC guarantees that the note content hasn't been tampered with, allowing the owner or authorized users to detect any changes.

**SR3 (Integrity 2)**: HMAC also ensures that no note is missing or altered. You can also use a version control mechanism to track versions of notes and check for any missing notes.

**SR4 (Authentication)**: TLS ensures that only authenticated and authorized users can access the server. HMAC ensures that only the owner or authorized users can interact with and verify the notes.

**SRA1 (Authentication)**: The firewall and TLS ensure that only authenticated users can access the note's, protecting it from unauthorized access.

**SRA2 (Integrity 1)**: HMAC ensures that anyone with access to the note can verify its integrity.

**SRA3 (Integrity 2)**: HMAC ensures the integrity of each version, allowing users to verify the integrity of shared notes.

### 2.3.2. Attacker Model

#### Trust Levels

- **Fully Trusted Entities:**
  - **VM3 (Server Machine):** As the central hub processing client requests and interacting with the database, it is fully trusted to handle sensitive operations and enforce security measures.
  
  - **VM4 (Database Machine):** Responsible for securely storing encrypted application data, it is fully trusted to maintain data confidentiality and integrity.

- **Partially Trusted Entities:**
  - **VM1 (Client Machine):** While it initiates legitimate requests to the server, it operates in a user-controlled environment, making it susceptible to compromise. Therefore, it is partially trusted.

  - **VM2 (Central Firewall):** Mediates traffic between the client and server, enforcing specific traffic rules. Its correct configuration is crucial, but misconfigurations or vulnerabilities could be exploited, rendering it partially trusted.

  - **VM5 (Database Firewall):** Acts as a security layer between the server and database, enforcing access controls. Similar to VM2, its effectiveness depends on proper configuration, making it partially trusted.

- **Untrusted Entities:**
  - **External Networks/Internet:** Any external network or internet source is considered untrusted, as it can be a source of potential attacks or unauthorized access attempts.

---

#### Capabilities

- **Network Scanning and Reconnaissance:**
  - An attacker can perform scanning to identify open ports, services, and potential vulnerabilities within the network.

- **Traffic Interception:**
  - If the attacker gains access to the network path, they might attempt to intercept unencrypted traffic. However, the use of TLS in our infrastructure mitigates this risk.

- **Exploiting Misconfigurations:**
  - The attacker can exploit any misconfigurations in firewalls (VM2, VM5) or services to gain unauthorized access or disrupt services.

- **Malware Deployment:**
  - If the client machine (VM1) is compromised, the attacker could deploy malware to intercept or alter legitimate requests.

---

#### Limitations

- **Encrypted Communication:**
  - The use of gRPC with TLS ensures that data in transit is encrypted, preventing attackers from easily reading or modifying the communication between VMs.

- **Strict Firewall Rules:**
  - Firewalls (VM2 and VM5) are configured to allow only specific traffic (e.g., TCP port 50052 for gRPC, TCP port 5432 for database access), limiting the attack surface and reducing the chances of unauthorized access.

- **Lack of Direct Access:**
  - Without valid credentials and proper authentication, the attacker cannot directly access the server (VM3) or database (VM4), especially since the database accepts connections only from the server.

#### 2.3.3. Solution Design and Implementation

In order to meet most of the security challenges our team decided to use RSA Keys to encrypt the **AES-GCM Key** and the **HMAC Key** so it could be sent to the server safely with the encrypted note. Everytime a user wants to edit/read a note, firstly the server will look on the **access_logs** table to see if the user has enough permissions, then proceed to decrypt the keys (since the server has every user public key, sent on the signup's) with the public key of the last user to modify the note and then encrypt the keys again with the public key of the user asking for the note. The user then can decrypt, with his private key, the **AES-GCM Key**, so he can decrypt the note, and *"validate"* it integrity by decrypting and using the **HMAC Key**. 
Apart from this our **Firewall** mediates traffic from client-server and server-database enforcing specific rules. (certain *ip's* and *ports*)
The **TLS** grants authentication.

## 3. Conclusion

- **Achievements**:  
    - created a library to secure files
    - created secure note-sharing solution  
    - developed infrastructure with robust security measures  

- **Requirements Satisfaction**:  
    - fully satisfied: secure infrastructue, secure sharing, key management
    - partially satisfied: local saved notes (everything is stored on the server only)   

- **Future Enhancements**:  
    - real-time access control analytics
    - add password hashing (password security) 
    - make the project able to change the title of a note

The project has demonstrated the importance of integrating cryptographic techniques with secure infrastructure design.

## 4. Bibliography

- Google. Gson Library Documentation. Last updated November 2024: [https://github.com/google/gson](https://github.com/google/gson)  
- Kali Linux : [https://www.kali.org/](https://www.kali.org/)
- The GRPC Authors. gRPC Documentation : [https://grpc.io/](https://grpc.io/)
- Offensive Security. Hping Tool Documentation : [https://www.kali.org/tools/hping3/](https://www.kali.org/tools/hping3/)

----
END OF REPORT
