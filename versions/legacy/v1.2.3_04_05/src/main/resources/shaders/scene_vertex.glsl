#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 FragPos;    // Pozycja w przestrzeni świata
out vec3 Normal;     // Normalna w przestrzeni świata
out vec2 TexCoords;  // Współrzędne tekstury

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    FragPos = vec3(model * vec4(aPos, 1.0)); // Transformuj pozycję do przestrzeni świata
    // Transformuj normalną do przestrzeni świata (używając macierzy normalnych)
    // Macierz normalnych to transponowana inwersja górnego lewego 3x3 macierzy modelu
    // Dla uproszczenia (jeśli nie ma niesymetrycznego skalowania) można użyć:
    Normal = mat3(transpose(inverse(model))) * aNormal;
    // Normal = normalize(vec3(model * vec4(aNormal, 0.0))); // Mniej poprawne, ale często działa

    TexCoords = aTexCoords; // Przekaż współrzędne tekstury

    gl_Position = projection * view * vec4(FragPos, 1.0); // Transformuj do przestrzeni Clip Space
}