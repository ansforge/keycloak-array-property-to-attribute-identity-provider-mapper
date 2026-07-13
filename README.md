# Keycloak - Array property to attribute - Identity provider mapper

This extension provides a Custom Mapper for OpenID Connect identity provider, adding capability to select property values from a JSON object array. Result is an array of values for the considered property.

```
Example :
{
  "objects": [
    {
      "propA": "value11",
      "propB": "value12",
      "propC": "value13",
      "propD": "value14"
    },
    {
      "propA": "value21",
      "propB": "value22",
      "propC": "value23",
      "propD": "value24"
    },
  ]
}

Key = objects.propB
Value = [ "value12", "value21" ]
```

## Compatibility

- Version `1.0.0` is compatible with Keycloak `26.X`.

## Install

As other [Keycloak SPI](https://www.keycloak.org/docs/latest/server_development/index.html#_implementing_spi),
* put jar file in ```/providers``` folder
* if Keycloak server il already started, stop it
* to take into account this new provider, launch following command ```/bin/kc.sh build```
* and start Keycloak server again ```/bin/kc.sh start```

## Settings

Connect to Keycloak admin console.
Select Identity Provider where you want to set up a new mapper :
![Select Identity Provider](/assets/keycloak-idp-mapper-1.jpg)
Click on **Add mapper** button and select **Array property to attribute** :
![Add new Identity Provider Mapper](/assets/keycloak-idp-mapper-2.jpg)
Set up your mapper config :
![Set Identity Provider Mapper Config](/assets/keycloak-idp-mapper-3.jpg)

## Development

### Build

To build your local package, execute following command ```mvnw package```

### Container

To test a provider, set version of your provider (jar file) in .env file :
```MAPPER_VERSION=1.0.0```

Then launch a Keycloak instance in a Docker container ```docker compose --env-file .env up```
