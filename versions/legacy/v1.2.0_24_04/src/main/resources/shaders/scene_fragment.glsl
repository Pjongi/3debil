#version 330 core
out vec4 FragColor;

in VS_OUT {
    vec3 FragPos;
    vec3 Normal;
    vec4 FragPosLightSpace;
    vec2 TexCoord; // <-- DODAJ WEJŚCIE UV Z VERTEX SHADERA
} fs_in;

// uniform vec3 objectColor; // <-- USUŃ LUB ZAKOMENTUJ
uniform vec3 viewPos;

struct DirLight {
    vec3 direction;
    vec3 color;
    float intensity;
};
uniform DirLight dirLight;

uniform sampler2D shadowMap;
uniform float shadowBias = 0.005;
uniform sampler2D textureSampler; // <-- DODAJ DEKLARACJĘ SAMPLERA

// Funkcja CalculateShadow (bez zmian)
float CalculateShadow(vec4 fragPosLightSpace)
{
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
     if(projCoords.z > 1.0)
         return 0.0;
    float shadow = currentDepth - shadowBias > closestDepth ? 1.0 : 0.0;
    return shadow;
}

void main()
{
    // Pobierz kolor bazowy z tekstury
    vec3 color = texture(textureSampler, fs_in.TexCoord).rgb; // <-- UŻYJ TEKSTURY

    vec3 normal = normalize(fs_in.Normal);

    // Oświetlenie Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * dirLight.color * dirLight.intensity;

    // Oświetlenie Diffuse
    vec3 lightDir = normalize(-dirLight.direction);
    float diff = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = diff * dirLight.color * dirLight.intensity;

    // Oświetlenie Specular (Blinn-Phong)
    float specularStrength = 0.5;
    vec3 viewDir = normalize(viewPos - fs_in.FragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), 32.0);
    vec3 specular = specularStrength * spec * dirLight.color * dirLight.intensity;

    // Cień
    float shadow = CalculateShadow(fs_in.FragPosLightSpace);

    // Kolor końcowy
    vec3 lighting = ambient + (1.0 - shadow) * (diffuse + specular);
    vec3 result = lighting * color; // Mnożenie oświetlenia przez kolor tekstury

    FragColor = vec4(result, 1.0);
}