#version 330 core

// --- Struktury Danych ---
struct MaterialInfo {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float reflectance; // = shininess
    bool hasDiffuseMap;
    bool hasSpecularMap;
};

struct DirLightInfo {
    vec3 direction; // Kierunek DO źródła światła
    vec3 color;
    float intensity;
    // bool enabled; // Można dodać flagę włącz/wyłącz
};

// Dla tłumienia
struct AttenuationInfo {
    float constant;
    float linear;
    float quadratic;
};

struct PointLightInfo {
    vec3 position;
    vec3 color;
    float intensity;
    AttenuationInfo att; // Współczynniki tłumienia
    // bool enabled;
};

struct SpotLightInfo {
    PointLightInfo pl; // Zawiera pozycję, kolor, intensywność, tłumienie
    vec3 direction;    // Kierunek reflektora
    float cutOffCos;      // Cosinus kąta wewnętrznego stożka
    float outerCutOffCos; // Cosinus kąta zewnętrznego stożka
    // bool enabled;
};

// --- Wejścia z Vertex Shadera ---
in vec3 FragPos;    // Pozycja fragmentu w przestrzeni świata
in vec3 Normal;     // Normalna fragmentu w przestrzeni świata
in vec2 TexCoords;  // Współrzędne tekstury

// --- Wyjście ---
out vec4 FragColor;

// --- Uniformy ---
uniform vec3 viewPos; // Pozycja kamery
uniform MaterialInfo material; // Właściwości materiału bieżącego obiektu

// Samplery
uniform sampler2D diffuseSampler;  // Jednostka 0
uniform sampler2D specularSampler; // Jednostka 1
uniform sampler2D shadowMapSampler; // Jednostka 2

// Światła
uniform DirLightInfo dirLight;
#define MAX_POINT_LIGHTS 4 // Musi pasować do stałej w Renderer.java
#define MAX_SPOT_LIGHTS 2  // Musi pasować do stałej w Renderer.java
uniform int numPointLights;
uniform int numSpotLights;
uniform PointLightInfo pointLights[MAX_POINT_LIGHTS];
uniform SpotLightInfo spotLights[MAX_SPOT_LIGHTS];

// Cienie (nadal tylko dla DirLight)
uniform mat4 lightSpaceMatrix;
uniform float shadowBias;

// --- Funkcje Pomocnicze ---

// Oblicza bazowy kolor diffuse (z tekstury lub materiału)
vec3 GetBaseDiffuse() {
    vec3 base = material.diffuse;
    if (material.hasDiffuseMap) {
        base *= texture(diffuseSampler, TexCoords).rgb;
    }
    return base;
}

// Oblicza bazowy kolor/współczynnik specular (z tekstury lub materiału)
vec3 GetBaseSpecular() {
    vec3 base = material.specular;
    if (material.hasSpecularMap) {
        // Zakładamy intensywność w kanale R mapy specular
        base *= texture(specularSampler, TexCoords).r;
    }
    return base;
}

// Oblicza tłumienie dla świateł punktowych/reflektorowych
float CalculateAttenuation(AttenuationInfo att, float distance) {
    return 1.0 / (att.constant + att.linear * distance + att.quadratic * (distance * distance));
}

// Funkcja cieni (bez zmian)
float CalculateShadow(vec4 fragPosLightSpace) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    if(projCoords.z > 1.0) return 0.0;
    float closestDepth = texture(shadowMapSampler, projCoords.xy).r;
    float currentDepth = projCoords.z;
    float shadow = currentDepth - shadowBias > closestDepth ? 1.0 : 0.0;
    return shadow;
}

// Oblicza wkład światła kierunkowego
vec3 CalcDirLightContribution(vec3 normal, vec3 viewDir, vec3 baseDiffuse, vec3 baseSpecular) {
    if (dirLight.intensity <= 0.0) return vec3(0.0); // Jeśli światło wyłączone

    vec3 lightDir = normalize(-dirLight.direction); // Kierunek OD fragmentu DO światła

    // Diffuse
    float diffFactor = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = dirLight.color * dirLight.intensity * diffFactor * baseDiffuse;

    // Specular (Blinn-Phong)
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float specFactor = pow(max(dot(normal, halfwayDir), 0.0), material.reflectance);
    vec3 specular = dirLight.color * dirLight.intensity * specFactor * baseSpecular;

    // Cień (tylko dla tego światła)
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);
    float shadow = CalculateShadow(fragPosLightSpace);

    // Zwróć oświetlenie (bez ambientu), uwzględniając cień
    return (1.0 - shadow) * (diffuse + specular);
}

// Oblicza wkład światła punktowego
vec3 CalcPointLightContribution(PointLightInfo light, vec3 normal, vec3 viewDir, vec3 baseDiffuse, vec3 baseSpecular) {
     if (light.intensity <= 0.0) return vec3(0.0);

    vec3 lightDir = normalize(light.position - FragPos); // Kierunek OD fragmentu DO światła
    float distance = length(light.position - FragPos);
    float attenuation = CalculateAttenuation(light.att, distance);

    // Diffuse
    float diffFactor = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = light.color * light.intensity * diffFactor * baseDiffuse;

    // Specular
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float specFactor = pow(max(dot(normal, halfwayDir), 0.0), material.reflectance);
    vec3 specular = light.color * light.intensity * specFactor * baseSpecular;

    return (diffuse + specular) * attenuation;
}

// Oblicza wkład światła reflektorowego
vec3 CalcSpotLightContribution(SpotLightInfo light, vec3 normal, vec3 viewDir, vec3 baseDiffuse, vec3 baseSpecular) {
     if (light.pl.intensity <= 0.0) return vec3(0.0);

    vec3 lightDir = normalize(light.pl.position - FragPos);
    float distance = length(light.pl.position - FragPos);
    float attenuation = CalculateAttenuation(light.pl.att, distance);

    // Sprawdzenie stożka reflektora
    float theta = dot(lightDir, normalize(-light.direction)); // Kąt między kierunkiem światła a wektorem do fragmentu
    float epsilon = light.cutOffCos - light.outerCutOffCos;
    float intensityFactor = clamp((theta - light.outerCutOffCos) / epsilon, 0.0, 1.0); // Płynne przejście na krawędziach

    if (intensityFactor <= 0.0) return vec3(0.0); // Poza stożkiem

    // Diffuse
    float diffFactor = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = light.pl.color * light.pl.intensity * diffFactor * baseDiffuse;

    // Specular
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float specFactor = pow(max(dot(normal, halfwayDir), 0.0), material.reflectance);
    vec3 specular = light.pl.color * light.pl.intensity * specFactor * baseSpecular;

    // Połącz i zastosuj tłumienie oraz współczynnik stożka
    return (diffuse + specular) * attenuation * intensityFactor;
}

// --- Główna Funkcja ---
void main()
{
    // Pobierz bazowe właściwości materiału
    vec3 baseDiffuse = GetBaseDiffuse();
    vec3 baseSpecular = GetBaseSpecular();

    // Przygotuj wektory normalnej i widoku
    vec3 norm = normalize(Normal);
    vec3 viewDir = normalize(viewPos - FragPos);

    // Oblicz składnik ambient (prosty model globalnego ambientu lub z dirLight)
    float ambientStrength = 0.1; // Globalna siła światła otoczenia
    vec3 ambient = ambientStrength * material.ambient;
    // Można też użyć dirLight:
    // vec3 ambient = (dirLight.color * dirLight.intensity * 0.1) * material.ambient;


    // Oblicz wkład światła kierunkowego (z uwzględnieniem cienia)
    vec3 result = CalcDirLightContribution(norm, viewDir, baseDiffuse, baseSpecular);

    // Dodaj wkład świateł punktowych
    for(int i = 0; i < numPointLights; i++) {
        result += CalcPointLightContribution(pointLights[i], norm, viewDir, baseDiffuse, baseSpecular);
    }

    // Dodaj wkład świateł reflektorowych
    for(int i = 0; i < numSpotLights; i++) {
        result += CalcSpotLightContribution(spotLights[i], norm, viewDir, baseDiffuse, baseSpecular);
    }

    // Dodaj ambient do finalnego wyniku
    vec3 finalColor = ambient + result;

    FragColor = vec4(finalColor, 1.0);
}