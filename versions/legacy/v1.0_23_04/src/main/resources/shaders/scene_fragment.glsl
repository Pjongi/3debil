#version 330 core
out vec4 FragColor;

in VS_OUT {
    vec3 FragPos;
    vec3 Normal;
    vec4 FragPosLightSpace;
} fs_in;

// Uniforms dla właściwości obiektu i sceny
uniform vec3 objectColor;
uniform vec3 viewPos; // Pozycja kamery

// Uniforms dla światła
struct DirLight {
    vec3 direction;
    vec3 color;
    float intensity;
};
uniform DirLight dirLight;

// Uniforms dla cieni
uniform sampler2D shadowMap; // Tekstura mapy cieni
uniform float shadowBias = 0.005; // Bias, aby uniknąć "shadow acne"

// Funkcja obliczająca cień
float CalculateShadow(vec4 fragPosLightSpace)
{
    // Wykonaj dzielenie perspektywiczne
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // Przekształć do zakresu [0,1]
    projCoords = projCoords * 0.5 + 0.5;

    // Pobierz najbliższą głębię z mapy cieni (jeśli współrzędne są w zakresie [0,1])
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    // Pobierz aktualną głębię fragmentu z perspektywy światła
    float currentDepth = projCoords.z;

     // Sprawdź, czy fragment jest poza zasięgiem mapy cieni
     if(projCoords.z > 1.0)
         return 0.0; // Poza zasięgiem - brak cienia

    // Oblicz cień z biasem
    float shadow = currentDepth - shadowBias > closestDepth ? 1.0 : 0.0;

    // --- Opcjonalnie: Proste PCF (Percentage Closer Filtering) dla miękkich cieni ---
    /*
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x)
    {
        for(int y = -1; y <= 1; ++y)
        {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - shadowBias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    */

    return shadow;
}

void main()
{
    vec3 color = objectColor;
    vec3 normal = normalize(fs_in.Normal);

    // --- Oświetlenie otoczenia (Ambient) ---
    float ambientStrength = 0.2; // Zmniejszone światło otoczenia
    vec3 ambient = ambientStrength * dirLight.color * dirLight.intensity;

    // --- Oświetlenie rozproszone (Diffuse) ---
    vec3 lightDir = normalize(-dirLight.direction); // Kierunek *do* światła
    float diff = max(dot(normal, lightDir), 0.0);
    vec3 diffuse = diff * dirLight.color * dirLight.intensity;

    // --- Oświetlenie lustrzane (Specular) ---
    float specularStrength = 0.5;
    vec3 viewDir = normalize(viewPos - fs_in.FragPos); // Kierunek do obserwatora
    // Odbicie Blinn-Phong jest wydajniejsze
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), 32.0); // 32 = shininess
    // Alternatywnie, standardowe odbicie Phonga:
    // vec3 reflectDir = reflect(-lightDir, normal);
    // float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
    vec3 specular = specularStrength * spec * dirLight.color * dirLight.intensity;

    // --- Cień ---
    float shadow = CalculateShadow(fs_in.FragPosLightSpace);

    // --- Kolor końcowy ---
    // Światło otoczenia nie jest wpływane przez cienie
    // Cienie wpływają na światło rozproszone i lustrzane
    vec3 lighting = ambient + (1.0 - shadow) * (diffuse + specular);
    vec3 result = lighting * color;

    FragColor = vec4(result, 1.0);
}