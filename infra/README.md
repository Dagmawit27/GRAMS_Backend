# Infrastructure

The initial deployment target is Docker on virtual machines. Copy the root .env.example to .env, then run:

`	ext
docker compose --env-file .env -f infra/docker-compose.yml up --build
` 

Kubernetes manifests are intentionally deferred until the VM deployment is operating reliably.
