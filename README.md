# Niord
Kotlin based application for drivers security and support, with community based features.

## Using docker compose
Make sure to have Docker installed <br>
https://www.docker.com/

Follow steps 1-2 of Server - Running in development, to update the dependencies.

1. Build the containers with:
   ```bash
   docker compose up -d --build
   ```
2. Open the Docker Desktop.
3. Look at the logs of each container, checking for errors, or connection statuses Ex. "Redis connected" on the server log for a succesfull connection with the Redis container.

If everything went right you should be able to access the server through _localhost_ with the port especified (port 80 in this case).
