#import <sodium:include/chunk_matrices.glsl>

uniform vec3 u_EyePos;
uniform vec3 u_PlayerPos;
uniform vec3 u_CameraPos;
uniform vec2 u_Resolution;
uniform sampler2D u_DepthBuffer;
uniform sampler2D u_PositionBuffer;

layout(location = 1) out vec3 position;

float IGN(vec2 coords) {
    return mod(52.9829189f * mod(0.06711056f * coords.x + 0.00583715f * coords.y, 1.0f), 1.0f);
}

// manhattan distance
float blockDist(int scale, vec3 dist) {
    vec3 off = floor(abs(dist * scale));
    return off.x + off.y + off.z;
}

void do_culling() {
    vec2 uv = gl_FragCoord.xy / u_Resolution;

//    vec3 prevCoord = texture(u_PositionBuffer, uv).xyz;
//    position = v_FragCoord;
//    if (prevCoord == vec3(0)) return;

//    gl_FragDepth = 0;

    if (texture(u_PositionBuffer, uv).r == 0) {
        position.r = 1;
        return;
    } else if (texture(u_PositionBuffer, uv).g == 1) {
        discard;
    }

    vec3 prevCoord = v_FragCoord;

    // all numbers are magic numbers if you don't know what they mean
    vec2 coord = mod(v_TexCoord / u_TexelSize - 0.5, 16);
    vec2 stipple = mod(v_TexCoord / u_TexelSize - 0.5, 4);
    vec3 texelToEye = round((prevCoord - u_EyePos) * 16) / 16 + u_CameraPos;
    vec3 texelToPlayer = round((prevCoord - u_PlayerPos) * 16) / 16 + u_CameraPos;
    vec3 cameraToEye = u_CameraPos - u_EyePos;
    vec3 cameraToPlayer = u_CameraPos - u_PlayerPos;
    vec3 fragDist = prevCoord - fract(u_CameraPos);
    vec3 blockletFragCoord =(blockDist(16, fragDist) < 64
        ? (blockDist(32, fragDist) < 64
            ? round((prevCoord + u_CameraPos) * 32) / 32
            : round((prevCoord + u_CameraPos) * 16) / 16)
        : round((prevCoord + u_CameraPos) * 8) / 8) - u_CameraPos;
    // cone apex behind camera for near clipping
    float dist = acos(dot(normalize(blockletFragCoord + normalize(u_EyePos - u_CameraPos) * 0.5), normalize(u_EyePos - u_CameraPos)));
    float angle = min(
        acos(dot(normalize(texelToEye), normalize(cameraToEye))),
        acos(dot(normalize(texelToPlayer), normalize(cameraToPlayer)))
    );
    float rad = 0.3 + (IGN(v_TexCoord) - 0.5) / 10;
    float border = length(prevCoord) / 16;
    bool center = coord.x >= border && coord.x <= 16 - border && coord.y >= border && coord.y <= 16 - border;
    if (
        // between eye and camera
        length(prevCoord) < length(u_CameraPos - u_EyePos) &&
        (
            // texel stipple
            dist < rad && (stipple.x > (dist - 0.1) * 20 || stipple.y > (dist - 0.1) * 20) ||
            // texel dither
            dist > rad && dist < rad + 0.1 && IGN(round(coord)) > (dist - rad) * 10 && center ||
            // outline
            dist < rad && center
        ) &&
        (prevCoord.y + u_CameraPos.y > u_EyePos.y) &&
        (angle < 3.141592653 / 4)
    ) {
        position.g = 1;
    } else {
        position.g = 0;
    }
}