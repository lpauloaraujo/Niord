# How to setup Redis

## Building the docker image and running
_(Make sure your working directory is the ./Redis folder, the root folder of this README)_

Make sure to have Docker installed <br>
https://www.docker.com/

1. Build the docker image with:
   ```bash
   docker build --no-cache -t redis ./
   ```
2. Open the Docker Desktop and run the image that should be named _redis_.
4. Define the password enviroment variable, with the key _REDIS_PASSWORD_ and define the port to 6379.
5. Run the image and check the logs for possible error.

If everything went right you should see the _Server initialized_ message in the logs.
