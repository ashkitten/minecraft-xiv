#version 330 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_material.glsl>
#import <sodium:include/chunk_matrices.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_FragDistance; // The fragment's distance from the camera (cylindrical and spherical)
in float fadeFactor;

in vec3 v_FragCoord;

flat in uint v_Material;

uniform sampler2D u_BlockTex; // The block texture

uniform vec4 u_FogColor; // The color of the shader fog
uniform vec2 u_EnvironmentFog; // The start and end position for environmental fog
uniform vec2 u_RenderFog; // The start and end position for border fog
uniform vec2 u_TexelSize;
uniform bool u_UseRGSS;

uniform vec3 u_EyePos;
uniform vec3 u_PlayerPos;
uniform vec3 u_CameraPos;

out vec4 fragColor; // The output fragment for the color framebuffer

vec4 sampleNearest(sampler2D sampler, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(sampler, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

// Rotated Grid Super-Sampling
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColor = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColor += textureLod(source, sampleUV, mipLevelExact);
    }
    rgssColor *= 0.25;

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

float IGN(vec2 coords)
{
    return mod(52.9829189f * mod(0.06711056f * coords.x + 0.00583715f * coords.y, 1.0f), 1.0f);
}

// manhattan distance
float blockDist(int scale) {
    vec3 off = floor(abs(v_FragCoord * scale - fract(u_CameraPos * scale)));
    return off.x + off.y + off.z;
}

float sdCylinder(vec3 p, vec3 a, vec3 b, float r)
{
    vec3  ba = b - a;
    vec3  pa = p - a;
    float baba = dot(ba,ba);
    float paba = dot(pa,ba);
    float x = length(pa*baba-ba*paba) - r*baba;
    float y = abs(paba-baba*0.5)-baba*0.5;
    float x2 = x*x;
    float y2 = y*y*baba;

    float d = (max(x,y)<0.0)?-min(x2,y2):(((x>0.0)?x2:0.0)+((y>0.0)?y2:0.0));

    return sign(d)*sqrt(abs(d))/baba;
}

float sdCappedCone( vec3 p, vec3 a, vec3 b, float ra, float rb )
{
    float rba  = rb-ra;
    float baba = dot(b-a,b-a);
    float papa = dot(p-a,p-a);
    float paba = dot(p-a,b-a)/baba;
    float x = sqrt( papa - paba*paba*baba );
    float cax = max(0.0,x-((paba<0.5)?ra:rb));
    float cay = abs(paba-0.5)-0.5;
    float k = rba*rba + baba;
    float f = clamp( (rba*(x-ra)+paba*baba)/k, 0.0, 1.0 );
    float cbx = x-ra - f*rba;
    float cby = paba - f;
    float s = (cbx<0.0 && cay<0.0) ? -1.0 : 1.0;
    return s*sqrt( min(cax*cax + cay*cay*baba,
    cbx*cbx + cby*cby*baba) );
}

void main() {
    vec4 color = u_UseRGSS ? sampleRGSS(u_BlockTex, v_TexCoord, u_TexelSize) : sampleNearest(u_BlockTex, v_TexCoord, u_TexelSize);
    color *= v_Color; // Apply per-vertex color modulator

    // all numbers are magic numbers if you don't know what they mean
    vec2 coord = mod(v_TexCoord / u_TexelSize, 16);
    vec2 stipple = mod(v_TexCoord / u_TexelSize, 4);

    vec3 texelToEye = round((v_FragCoord - u_EyePos) * 16) / 16 + u_CameraPos;
    vec3 fragToPlayer = v_FragCoord + u_CameraPos - u_PlayerPos;
    vec3 cameraToEye = u_CameraPos - u_EyePos;
    vec3 cameraToPlayer = u_CameraPos - u_PlayerPos;
    vec3 blockletFragCoord = blockDist(16) < 64 ? (
        blockDist(32) < 64
            ? round((v_FragCoord + u_CameraPos) * 64) / 64
            : round((v_FragCoord + u_CameraPos) * 32) / 32
    ) : round((v_FragCoord + u_CameraPos) * 16) / 16;
    float dist = acos(dot(normalize(blockletFragCoord - u_CameraPos), normalize(u_EyePos - u_CameraPos)));
    float angle = min(
        acos(dot(normalize(texelToEye), normalize(cameraToEye))),
        acos(dot(normalize(fragToPlayer), normalize(cameraToPlayer)))
    );
    float rad = 0.3 + (IGN(v_TexCoord) - 0.5) / 10;
    float border = length(v_FragCoord) / 16;
    bool center = coord.x >= border && coord.x <= 16 - border && coord.y >= border && coord.y <= 16 - border;
    // between eye and camera
    if (length(v_FragCoord) < length(u_CameraPos - u_EyePos) &&
        // texel stipple
        (dist < rad && (stipple.x > (dist - 0.1) * 20 || stipple.y > (dist - 0.1) * 20) ||
            // texel dither
            dist > rad && dist < rad + 0.1 && (
                // base level
                IGN(round(coord)) > (dist - rad) * 10 ||
                // level 2 (at 4 blocks)
                blockDist(16) < 64 && IGN(round(coord * 2)) > (dist - rad) * 10 ||
                // level 3 (at 2 blocks)
                blockDist(32) < 64 && IGN(round(coord * 4)) > (dist - rad) * 10
            ) && center ||
            // screen-space stipple when frag too close
            //dist < rad + 0.1 && length(v_FragCoord) < 1 && (mod(gl_FragCoord.x + IGN(gl_FragCoord.xy), 2) > 1 || mod(gl_FragCoord.y + IGN(gl_FragCoord.xy), 2) > 1) ||
            // outline
            dist < rad && center) &&
        (v_FragCoord.y + u_CameraPos.y > u_EyePos.y) &&
        (angle < 3.141592653 / 4)
    ) {
        discard;
    }

    #ifdef USE_FRAGMENT_DISCARD
    if (color.a < _material_alpha_cutoff(v_Material)) {
        discard;
    }
    #endif

    fragColor = _linearFog(color, v_FragDistance, u_FogColor, u_EnvironmentFog, u_RenderFog, fadeFactor);
}