This `README.md` is designed to get your environment running from scratch and successfully testing the OAuth2 Password Grant flow using your Spring Boot application and Keycloak.

---

# 🚀 OAuth2 Token Test Project

This project demonstrates how to retrieve an OAuth2 Access Token using the **Resource Owner Password Credentials (ROPC)** grant type with the Nimbus OAuth2 SDK and a local Keycloak instance.

## 🛠 Prerequisites

* **Docker & Docker Compose** installed.
* **Java 17+** and **Maven/Gradle** (for the Spring Boot app).
* An API client like **Postman** or **cURL**.

---

## 1️⃣ Step 1: Spin up the Infrastructure

Run the following command to start Keycloak and its PostgreSQL database:

```bash
docker-compose -f src/main/docker/keycloak.yaml -p docker up -d

```

* **Admin Console:** `http://localhost:8090`
* **Username:** `admin`
* **Password:** `admin`

---

## 2️⃣ Step 2: Keycloak Configuration

Before running the code, you must configure Keycloak to allow the "Password" grant.

### A. Create the Realm

1. Log in to the Admin Console.
2. Click the **Master** dropdown in the top-left corner and click **Create Realm**.
3. Name it **`spring-oauth-test-realm`** and click **Create**.

### B. Create the Client

1. Go to **Clients** -> **Create client**.
2. **Client ID:** `spring-oauth-test-client`
3. Click **Next**.
4. **Capability Config:** Toggle **Direct Access Grants** to **ON**. (This enables `grant_type=password`).
5. Click **Save**.

### C. Create and Setup the User

1. Go to **Users** -> **Create new user**.
2. **Username:** `spring-oauth-test-user`
3. **Required Details (Mandatory):** Fill in **Email**, **First Name**, and **Last Name**. Set **Email Verified** to **On**.
4. Click **Create**.
5. Go to the **Credentials** tab -> **Set Password**.
6. Set password to `spring-oauth-test-pwd`.
7. **Toggle "Temporary" to OFF** (This is critical).
8. Click **Save**.

---

## 3️⃣ Step 3: Application Configuration

Update your `application.yml` or `application.properties` to point to your local Keycloak instance.

```yaml
oauth:
  issureUrl: http://localhost:8090/realms/spring-oauth-test-realm
  clientId: spring-oauth-test-client

```

---

## 4️⃣ Step 4: Run & Test

Start your Spring Boot application. Once the application is running, trigger the token retrieval via the `TokenController`.

### The Request

Send a `GET` request to your controller:

**URL:**
`GET http://localhost:8081/token-controller?userName=spring-oauth-test-user&password=spring-oauth-test-pwd`

### Expected Response

If successful, you will receive a `200 OK` with the full OAuth2 response. Your console will also print the extracted tokens:

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCIg...",
  "refreshToken": "eyJhbGciOiJ...",
  "tokenType": "Bearer",
  "expiresIn": 60
}

```

---

## ⚠️ Troubleshooting

| Error | Cause | Solution |
| --- | --- | --- |
| `invalid_grant: Account is not fully set up` | Missing user profile info | Ensure First Name, Last Name, and Email are filled in Keycloak. |
| `invalid_grant: Invalid grant_type` | Capability disabled | Ensure "Direct Access Grants" is toggled ON in Client settings. |
| `Connection Refused` | Docker not running | Check `docker ps` to ensure Keycloak is healthy on port 8080. |
| `401 Unauthorized` | Temporary Password | Ensure the password "Temporary" toggle was turned OFF during setup. |

---

## 📂 Project Structure

* **`OauthConfig`**: Holds the Issuer URL and Client ID.
* **`TokenController`**: Uses Nimbus SDK to resolve metadata and exchange credentials for tokens.
* **`docker-compose.yml[keycloak.yaml](src/main/docker/keycloak.yaml)`**: Provisions the local IAM environment.
