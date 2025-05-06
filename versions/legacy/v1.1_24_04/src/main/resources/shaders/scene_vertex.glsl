#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal; // Potrzebujemy normalnych do oświetlenia

out VS_OUT {
    vec3 FragPos; // Pozycja fragmentu w przestrzeni świata
    vec3 Normal;  // Normalna w przestrzeni świata
    vec4 FragPosLightSpace; // Pozycja fragmentu w przestrzeni światła (do cieni)
} vs_out;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat4 lightSpaceMatrix; // Macierz przekształcająca do przestrzeni światła

void main()
{
    // Pozycja w przestrzeni świata
    vs_out.FragPos = vec3(model * vec4(aPos, 1.0));

    // Normalna w przestrzeni świata (użyj macierzy normalnych, aby obsłużyć niesymetryczne skalowanie)
    // Dla uproszczenia zakładamy równomierne skalowanie lub brak skalowania:
    vs_out.Normal = mat3(transpose(inverse(model))) * aNormal;
    // Alternatywnie, jeśli nie ma niesymetrycznego skalowania:
    // vs_out.Normal = mat3(model) * aNormal;

    // Pozycja w przestrzeni widoku/kamery (Clip Space)
    gl_Position = projection * view * vec4(vs_out.FragPos, 1.0);

    // Pozycja w przestrzeni światła (do cieni)
    vs_out.FragPosLightSpace = lightSpaceMatrix * vec4(vs_out.FragPos, 1.0);
}