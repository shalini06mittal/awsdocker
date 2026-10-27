
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
