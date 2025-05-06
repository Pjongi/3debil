#version 330 core
layout (location = 0) in vec3 aPos; // Tylko pozycja jest potrzebna

uniform mat4 lightSpaceMatrix;
uniform mat4 model;

void main()
{
    // Przekształć pozycję wierzchołka do przestrzeni światła
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}