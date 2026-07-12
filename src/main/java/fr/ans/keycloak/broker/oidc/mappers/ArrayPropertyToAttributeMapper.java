package fr.ans.keycloak.broker.oidc.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

/**
 * Mapper IdP OIDC personnalisé.
 * Prend un claim qui est un tableau d'objets JSON (ex: "roles":[{"id":1,"name":"admin"},...])
 * et alimente un attribut utilisateur multivalué avec les valeurs d'une propriété donnée
 * de chaque objet (ex: "roleNames": ["admin", "editor"]).
 */
public class ArrayPropertyToAttributeMapper extends AbstractClaimMapper implements IdentityProviderMapper {

    public static final String PROVIDER_ID = "array-property-to-attribute-mapper";

    public static final String JSON_PROPERTY = "jsonProperty";
    public static final String USER_ATTRIBUTE = "user.attribute";

    public static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID
    };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        ProviderConfigProperty claimProperty = new ProviderConfigProperty();
        claimProperty.setName(CLAIM); // "claim", hérité de AbstractClaimMapper
        claimProperty.setLabel("Claim (tableau d'objets)");
        claimProperty.setHelpText(
                "Nom du claim source. Doit être un tableau d'objets JSON, ex: 'roles'. " +
                "Notation pointée possible pour des claims imbriqués, ex: 'data.roles'.");
        claimProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(claimProperty);

        ProviderConfigProperty jsonPropertyProperty = new ProviderConfigProperty();
        jsonPropertyProperty.setName(JSON_PROPERTY);
        jsonPropertyProperty.setLabel("Propriété à extraire");
        jsonPropertyProperty.setHelpText(
                "Nom de la propriété à extraire de chaque objet du tableau, ex: 'name'.");
        jsonPropertyProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(jsonPropertyProperty);

        ProviderConfigProperty attributeProperty = new ProviderConfigProperty();
        attributeProperty.setName(USER_ATTRIBUTE);
        attributeProperty.setLabel("Attribut utilisateur cible");
        attributeProperty.setHelpText(
                "Nom de l'attribut utilisateur Keycloak à alimenter (sera multivalué), ex: 'roleNames'.");
        attributeProperty.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(attributeProperty);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Array Property to Attribute Importer";
    }

    @Override
    public String getHelpText() {
        return "Extrait une propriété de chaque objet d'un claim de type tableau d'objets "
                + "et l'importe comme attribut utilisateur multivalué.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
                                             IdentityProviderMapperModel mapperModel,
                                             BrokeredIdentityContext context) {
        List<String> values = extractValues(mapperModel, context);
        context.setUserAttribute(mapperModel.getConfig().get(USER_ATTRIBUTE), values);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
                               IdentityProviderMapperModel mapperModel,
                               BrokeredIdentityContext context) {
        setUserAttribute(user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
                                    IdentityProviderMapperModel mapperModel,
                                    BrokeredIdentityContext context) {
        setUserAttribute(user, mapperModel, context);
    }

    private void setUserAttribute(UserModel user, IdentityProviderMapperModel mapperModel,
                                   BrokeredIdentityContext context) {
        String attributeName = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (attributeName == null || attributeName.trim().isEmpty()) {
            return;
        }
        List<String> values = extractValues(mapperModel, context);
        user.setAttribute(attributeName, values);
    }

    /**
     * Récupère le claim brut (via le mécanisme standard de Keycloak, qui gère
     * la notation pointée et va chercher dans access token / id token / userinfo),
     * puis extrait la propriété demandée de chaque élément du tableau.
     */
    private List<String> extractValues(IdentityProviderMapperModel mapperModel,
                                        BrokeredIdentityContext context) {
        Object rawClaimValue = getClaimValue(mapperModel, context);
        String propertyName = mapperModel.getConfig().get(JSON_PROPERTY);

        List<String> result = new ArrayList<>();
        if (rawClaimValue == null || propertyName == null || propertyName.trim().isEmpty()) {
            return result;
        }

        // Normalise en JsonNode quel que soit le type renvoyé par Keycloak
        // (peut être List<Map>, JsonNode, tableau, etc. selon la provenance du claim)
        JsonNode arrayNode = OBJECT_MAPPER.valueToTree(rawClaimValue);

        if (arrayNode == null || !arrayNode.isArray()) {
            return result;
        }

        for (JsonNode element : arrayNode) {
            if (element == null || element.isNull()) {
                continue;
            }
            JsonNode propNode = element.get(propertyName);
            if (propNode != null && !propNode.isNull()) {
                result.add(propNode.isTextual() ? propNode.asText() : propNode.toString());
            }
        }

        return result;
    }
}