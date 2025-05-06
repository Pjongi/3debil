#version 330 core

// Struktury wejściowe (pasujące do vertex shadera)
in vec3 FragPos;    // Pozycja fragmentu w przestrzeni świata
in vec3 Normal;     // Normalna fragmentu w przestrzeni świata
in vec2 TexCoords;  // Współrzędne tekstury

// Uniformy
uniform vec3 viewPos; // Pozycja kamery

// Struktura materiału
struct MaterialInfo {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float reflectance; // = shininess
    bool hasDiffuseMap;
    bool hasSpecularMap;
};
uniform MaterialInfo material;

// Struktura światła kierunkowego
struct DirLightInfo {
    vec3 direction;
    vec3 color;
    float intensity;
};
uniform DirLightInfo dirLight;

// Samplery tekstur (numery jednostek ustawiane w Renderer.setupScenePerFrameUniforms)
uniform sampler2D diffuseSampler;  // Jednostka 0
uniform sampler2D specularSampler; // Jednostka 1
uniform sampler2D shadowMapSampler; // Jednostka 2

// Uniformy cieni
uniform mat4 lightSpaceMatrix;
uniform float shadowBias;

// Wyjście shadera
out vec4 FragColor;

// Funkcja obliczająca oświetlenie kierunkowe (np. Blinn-Phong)
vec3 CalcDirLight(vec3 normal, vec3 viewDir, vec3 lightDir, vec3 baseDiffuse, vec3 baseSpecular) {
    // Ambient
    // Używamy koloru światła * intensywność jako światło otoczenia (proste)
    float ambientStrength = 0.15; // Zmniejsz siłę ambientu, jeśli materiał ma własny
    vec3 ambient = (dirLight.color * dirLight.intensity * ambientStrength) * material.ambient;

    // Diffuse
    float diff = max(dot(normal, -lightDir), 0.0);
    vec3 diffuse = (dirLight.color * dirLight.intensity * diff) * baseDiffuse;

    // Specular (Blinn-Phong)
    vec3 halfwayDir = normalize(-lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), material.reflectance);
    vec3 specular = (dirLight.color * dirLight.intensity * spec) * baseSpecular;

    return (ambient + diffuse + specular);
}

// Funkcja obliczająca cień
float CalculateShadow(vec4 fragPosLightSpace) {
    // Przekształć do Normalized Device Coordinates [-1, 1]
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // Przekształć do zakresu [0, 1] dla współrzędnych tekstury mapy cieni
    projCoords = projCoords * 0.5 + 0.5;

    // Pobierz najbliższą głębokość z mapy cieni (z perspektywy światła)
    float closestDepth = texture(shadowMapSampler, projCoords.xy).r;
    // Pobierz aktualną głębokość fragmentu (z perspektywy światła)
    float currentDepth = projCoords.z;

    // Sprawdź, czy fragment jest poza obszarem mapy cieni
    if(projCoords.z > 1.0) return 0.0; // Jest poza far plane światła

    // Oblicz cień (z biasem, aby uniknąć shadow acne)
    float shadow = currentDepth - shadowBias > closestDepth ? 1.0 : 0.0; // 1.0 = w cieniu, 0.0 = oświetlony

    return shadow;
}


void main()
{
    // Pobierz bazowe kolory z materiału i/lub tekstur
    vec3 baseDiffuse = material.diffuse;
    if (material.hasDiffuseMap) {
        baseDiffuse *= texture(diffuseSampler, TexCoords).rgb;
    }

    vec3 baseSpecular = material.specular;
    if (material.hasSpecularMap) {
         // Załóżmy, że mapa specular przechowuje intensywność w kanale R
        baseSpecular *= texture(specularSampler, TexCoords).r;
    }

    // Przygotuj wektory dla oświetlenia
    vec3 norm = normalize(Normal);
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 lightDir = normalize(dirLight.direction); // Kierunek *DO* światła jest przeciwny

    // Oblicz wynikowe oświetlenie
    vec3 lightingResult = CalcDirLight(norm, viewDir, lightDir, baseDiffuse, baseSpecular);

    // Oblicz cień
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);
    float shadowFactor = CalculateShadow(fragPosLightSpace);

    // Zastosuj cień (zmniejsz oświetlenie dla fragmentów w cieniu)
    // Zachowaj component ambient, aby cienie nie były całkowicie czarne
    float ambientStrength = 0.15;
    vec3 finalColor = ambientStrength * material.ambient * dirLight.color * dirLight.intensity + (1.0 - shadowFactor) * (lightingResult - ambientStrength * material.ambient * dirLight.color * dirLight.intensity) ;
    //vec3 finalColor = (1.0 - shadowFactor) * lightingResult; // Prostsze, ciemniejsze cienie


    FragColor = vec4(finalColor, 1.0);
    // FragColor = vec4(baseDiffuse, 1.0); // Debug: Pokaż tylko kolor diffuse
    // FragColor = texture(diffuseSampler, TexCoords); // Debug: Pokaż tylko teksturę diffuse
     // FragColor = vec4(vec3(shadowFactor), 1.0); // Debug: Pokaż tylko cień (biały=w cieniu)
}