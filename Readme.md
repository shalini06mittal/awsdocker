# Deploying the Mission App on AWS EC2 with Docker

Repository: <https://github.com/shalini06mittal/awsdocker>

The repo root contains `docker-compose.yml` and `postgres.sql`, plus the `mission-ui` (Angular), Spring Boot and NestJS projects.

> **Replace every EC2 hostname/IP below** (`ec2-3-235-8-183.compute-1.amazonaws.com` / `3.235.8.183`) with your own instance's public DNS / public IP.

---

## Part 1: Angular changes (`mission-ui`)

### 1. Create `environment.ts`

Create `src/environments/environment.ts` with the content below. Replace the URLs with your EC2 public DNS.

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://ec2-3-235-8-183.compute-1.amazonaws.com:8080',
  authUrl: 'http://ec2-3-235-8-183.compute-1.amazonaws.com:3000/auth'
};
```

### 2. Create the `Dockerfile`

Create `Dockerfile` inside `mission-ui`:

```dockerfile
# Stage 1: Build the Angular application
FROM node:22-alpine AS build

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy Angular source and build configuration
COPY . .

# Build Angular for production
RUN npm run build -- --configuration production

# Normalize the Angular build output directory.
# Modern Angular projects often output to dist/<project>/browser.
RUN set -eux; \
    mkdir -p /output; \
    if [ -d dist/mission-ui/browser ]; then \
      cp -a dist/mission-ui/browser/. /output/; \
    elif [ -d dist/mission-ui ]; then \
      cp -a dist/mission-ui/. /output/; \
    else \
      echo "Angular build output not found. Check angular.json outputPath."; \
      find dist -maxdepth 4 -type f | head -50; \
      exit 1; \
    fi

# Stage 2: Serve the compiled files using Nginx
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/mission-ui/browser/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 3. Create `nginx.conf`

Create `nginx.conf` in the same folder as `package.json`:

```properties
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 4. Check `generated.api.base.service.ts`

Open `src/app/generated.api.base.service.ts` and make sure it reads the API URL from the environment file (adjust the relative path to match your folder depth):

```typescript
import { environment } from '../../../environments/environment';
```

---

## Part 2: Spring Boot changes

Update `application.properties` so the app reaches Postgres through the Docker Compose service name (`postgres`), not `localhost`:

```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/paysprint
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
```

`POSTGRES_USER` and `POSTGRES_PASSWORD` must be supplied through the `environment` section of the service in `docker-compose.yml` (or a `.env` file).

---

## Part 3: NestJS changes

Update `main.ts` to allow CORS requests from the Angular app:

```typescript
app.enableCors({
  origin: [
    'http://3.235.8.183',
    'http://ec2-3-235-8-183.compute-1.amazonaws.com',
  ],
  methods: ['GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
});
```

---

## Part 4: Database and Compose files

Refer to these files in the GitHub repo root:

- [`postgres.sql`](https://github.com/shalini06mittal/awsdocker/blob/main/postgres.sql): database schema/seed script
- [`docker-compose.yml`](https://github.com/shalini06mittal/awsdocker/blob/main/docker-compose.yml): defines all services (Postgres, Spring Boot, NestJS, Angular UI)

---

## Part 5: Install Docker on EC2 (m6a.large, Ubuntu 24.04 LTS)

### 1. Connect to the instance

```bash
ssh -i /path/to/your-key.pem ubuntu@<EC2_PUBLIC_DNS>
```

### 2. Remove any conflicting packages

```bash
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do
  sudo apt-get remove -y $pkg
done
```

### 3. Set up Docker's official apt repository

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
```

### 4. Install Docker Engine and the Compose plugin

```bash
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 5. Run Docker without `sudo`

```bash
sudo usermod -aG docker $USER
newgrp docker
```

(Or log out and log back in.)

### 6. Verify the installation

```bash
docker --version
docker compose version
docker run hello-world
```

### 7. Enable Docker on boot

```bash
sudo systemctl enable docker
sudo systemctl enable containerd
```

---

## Part 6: Deploy the application

### 1. Clone the repository

```bash
sudo apt-get install -y git
git clone https://github.com/shalini06mittal/awsdocker.git
cd awsdocker
```

### 2. Create a `.env` file (if `docker-compose.yml` uses variables)

```bash
cat > .env <<'EOF'
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
EOF
```

### 3. Build and start the containers

```bash
docker compose up -d --build
```

### 4. Check status and logs

```bash
docker compose ps
docker compose logs -f
```

### 5. Stop or rebuild

```bash
docker compose down            # stop and remove containers
docker compose up -d --build   # rebuild after code changes
```

---

## Part 7: EC2 Security Group checklist

In the AWS Console, go to **EC2 > Instances > your instance > Security > Security groups > Edit inbound rules**, and allow:

| Port | Purpose                  | Source                                          |
|------|--------------------------|-------------------------------------------------|
| 22   | SSH                      | Your IP only                                    |
| 80   | Angular UI (Nginx)       | 0.0.0.0/0                                       |
| 8080 | Spring Boot API          | 0.0.0.0/0 (browser calls it directly)           |
| 3000 | NestJS auth service      | 0.0.0.0/0 (browser calls it directly)           |
| 5432 | Postgres                 | **Do not open**; keep it internal to Docker     |

Then open the app at `http://<EC2_PUBLIC_DNS>`.

---

## Troubleshooting

- **CORS errors in the browser:** confirm the NestJS `origin` list matches exactly how you open the app (DNS vs IP, `http` vs `https`, no trailing slash).
- **Spring Boot can't connect to the database:** confirm the Compose service is named `postgres` and the app is on the same Compose network.
- **Angular still calls the old URL:** rebuild the image with `docker compose up -d --build` so the new `environment.ts` is compiled in.
- **Public DNS changed after a stop/start:** EC2 public DNS/IP changes unless you attach an Elastic IP. Update `environment.ts` and the CORS origins if it changes.
- **Dockerfile note:** the `/output` normalization step in Stage 1 is not used by Stage 2, which copies directly from `/app/dist/mission-ui/browser/`. That works as long as your build outputs to `dist/mission-ui/browser`. If your `angular.json` outputPath differs, update the `COPY --from=build` line.