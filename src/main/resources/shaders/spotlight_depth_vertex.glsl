#version 330 core
layout (location = 0) in vec3 aPos;

uniform mat4 model;
uniform mat4 lightSpaceMatrix; // Będzie to lightProjection * lightView (dla konkretnej ściany)

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}