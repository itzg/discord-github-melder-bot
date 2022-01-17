This is a Discord bot that provides an application command for Discord users to link their Github user and automatically apply a contributor role to those users that have contributed to the configured repositories.

# Installation

## [Create Discord application](https://discord.com/developers/applications)

Go to the Bot section and create a bot there. 
 
When generating the application invite URL, enable the scopes
  - bot
    - Manage Roles
  - applications.commands

The generated URL should end with `permissions=268435456&scope=bot%20applications.commands`

## [Create Github OAuth2 application](https://github.com/settings/developers)

- Generate a client secret
- Set "Authorization callback URL" to the base URL of your bot with the path `/login/oauth2/code/github`

## Optional: [Generate personal access token](https://github.com/settings/tokens)

If you would like the bot to install a webhook into each Github repository, allocate a personal access token with the scope
- `admin:repo_hook`

It can be given a very short expiration since it is only needed during initial setup and any time more repositories are added to the configuration.

## MongoDB

**Version:** 4.x

**Database:** discord-github-melder

> **INFO** Database can be created from mongo CLI with `use discord-github-melder`

Create user for the bot application's access, such as:

```
db.createUser({
  user: "dev",
  pwd: "dev",
  roles: ["readWrite"]
})
```

## Configure container

### Image

[`itzg/discord-github-melder-bot:{tag}`](https://hub.docker.com/r/itzg/discord-github-melder-bot)

### Ports

- `8080` : For HTTP requests

### Environment Variables

> **WARNING** Be sure to securely declare application IDs and tokens

| Variable                   | Required | Description                                                                    |
|----------------------------|----------|--------------------------------------------------------------------------------|
| GITHUB_CLIENT_ID           | Yes      | Github OAuth2 client ID                                                        |
| GITHUB_CLIENT_SECRET       | Yes      | Github OAuth2 client secret                                                    |
| MONGO_HOST                 | Yes      |                                                                                |
| MONGO_USERNAME             | Yes      | A user with read/write, create collection/index permissions                    |
| MONGO_PASSWORD             | Yes      |                                                                                |
| APP_DISCORD_APPLICATION_ID | Yes      | From `https://discord.com/developers/applications/{id}/information`            |
| APP_DISCORD_BOT_TOKEN      | Yes      | From `https://discord.com/developers/applications/{id}/bot`                    |
| APP_GITHUB_REPOS           | Yes      | Comma-separated list of `{org}/{name}`                                         |
| APP_BASE_URL               | Yes      | Publicly accessible URL that is routed to this container.                      |
| APP_GITHUB_ACCESS_TOKEN    | No       | Personal access token used at startup to install missing webhook declarations. |
| APP_GITHUB_WEBHOOK_SECRET  | No       | If set, the installed webhook will use the given secret for signing calls.     |
| LOGGING_LEVEL_APP          | No       | Change application logging level, such as `debug`                              |

There are more optional, lower priority properties declared [here](src/main/java/me/itzg/melderbot/config/AppProperties.java).
