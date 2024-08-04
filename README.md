# GitHub Repo Api

- [Description](#description)
- [Technologies](#technologies)
- [Features](#features)
- [How to Run](#how-to-run)
- [API Endpoints](#api-endpoints)
- [Example Response](#example-response)
- [Error Handling](#error-handling)
- [Project Status](#project-status)
- [Author](#author)

## Description

This project provides an API to retrieve all non-fork repositories for a specified GitHub user, 
along with detailed information about each branch in those repositories. 
It handles cases where the user does not exist and ensures no sensitive data (like GitHub tokens) is committed to the repository.

## Technologies

- Java 21
- Spring Boot 3
- Lombok
- Devtools
- WebClient
- Junit 5

## Features

- Fetch all non-fork repositories for a given GitHub user.
- Retrieve branch names and last commit SHA for each repository.
- Error handling for non-existent users and invalid usernames.
- Secure handling of API tokens to avoid exposure in the codebase.

## How to Run

1. Make Sure You have Java Installed on your computer.
2. Clone this repository to your computer
   ```sh
   git clone https://github.com/your-username/github-repo-fetcher.git
3. Go to the project folder and run the application with the command:
   ```sh
   ./mvnw spring-boot:run
5. The Application will run on port 8080.
6. Now you can test it with Postman.

## API Endpoints

GET /api/github/user/{username}/repositories: Fetch all non-fork repositories for the given GitHub user.

## Example Response

```json
[
    {
        "name": "repo1",
        "ownerLogin": "user",
        "branches": [
            {
                "name": "main",
                "lastCommitSha": "sha123"
            }
        ]
    }
]
```

## Error Handling

```json
{
    "status": 404,
    "message": "User not found: {username}"
}
```

## Project Status

This project is currently in active development. Future enhancements may include more detailed error handling, caching mechanisms, and extended API functionality.

## Author

The project was created by Piotr Rakocki.
