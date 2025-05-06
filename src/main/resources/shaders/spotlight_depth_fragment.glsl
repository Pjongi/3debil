#version 330 core
// Nie potrzebujemy out FragColor, bo piszemy tylko do bufora głębi

// uniform vec3 lightPos; // Pozycja światła w przestrzeni świata
// uniform float far_plane; // Daleka płaszczyzna odcięcia światła (jego zasięg)

// gl_FragCoord.z jest wartością głębi w zakresie [0,1] po transformacji perspektywicznej.
// To jest to, co domyślnie trafia do bufora głębi.
// Dla map cieni punktowych czasami normalizuje się odległość do far_plane.
// Na razie zostawimy domyślne pisanie gl_FragCoord.z, co jest standardowe dla map głębi.

void main() {
    // Domyślnie, bufor głębi otrzyma znormalizowaną wartość gl_FragCoord.z.
    // Jeśli potrzebowalibyśmy zapisać zlinearyzowaną odległość (co jest częste
    // przy cieniach punktowych, aby mieć lepszą precyzję), musielibyśmy to obliczyć:
    // float lightDistance = length(fs_in.FragPosLightSpace - lightPos); // FragPosLightSpace to pozycja wierzchołka w przestrzeni światła
    // lightDistance = lightDistance / far_plane; // Normalizuj do [0,1]
    // gl_FragDepth = lightDistance;
    // Jednak na razie, pozwólmy OpenGL zapisać standardową głębokość.
    // OpenGL automatycznie zapisze głębokość, więc ten shader może być pusty,
    // jeśli nie wykonujemy specjalnych obliczeń głębi.
    // Zostawiamy go, gdybyśmy chcieli dodać logikę far_plane w przyszłości.
}