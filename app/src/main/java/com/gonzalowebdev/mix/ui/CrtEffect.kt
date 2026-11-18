package com.gonzalowebdev.mix.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Efecto de TV de tubo / VHS con curvatura real de pantalla (AGSL, Android 13 o superior).
 */
private const val CRT_AGSL = """
uniform shader content;
uniform float2 res;
uniform float time;
uniform float vhs;

float rand(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / res;
    float2 c = uv * 2.0 - 1.0;
    float k = mix(0.07, 0.03, vhs);
    c *= float2(1.0 + c.y * c.y * k, 1.0 + c.x * c.x * k * 1.3);
    float2 suv = c * 0.5 + 0.5;
    if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }
    float2 sp = suv * res;
    if (vhs > 0.5) {
        float band = exp(-abs(suv.y - fract(time * 0.08)) * 38.0);
        sp.x += band * 14.0 * sin(suv.y * 90.0 + time * 18.0);
        sp.x += (rand(float2(floor(suv.y * 180.0), floor(time * 12.0))) - 0.5) * 1.5;
    }
    float ab = mix(1.4, 3.2, vhs);
    half4 base = content.eval(sp);
    half r = content.eval(sp + float2(ab, 0.0)).r;
    half b = content.eval(sp - float2(ab, 0.0)).b;
    half3 col = half3(r, base.g, b);
    float scan = 0.5 + 0.5 * sin(suv.y * 260.0 * 6.28318);
    col *= half(mix(0.80, 1.0, scan));
    float vig = 1.0 - smoothstep(0.35, 1.35, length(c));
    col *= half(mix(0.5, 1.0, vig));
    col *= half(1.0 + 0.02 * sin(time * 40.0));
    float n = rand(sp + float2(time * 61.0, time * 37.0));
    col += half((n - 0.5) * mix(0.05, 0.12, vhs));
    if (vhs > 0.5) {
        half l = dot(col, half3(0.299, 0.587, 0.114));
        col = mix(half3(l), col, half(0.8));
    }
    return half4(col, 1.0);
}
"""

@RequiresApi(33)
class CrtShader private constructor(private val shader: RuntimeShader) {

    fun effect(width: Float, height: Float, time: Float, vhs: Boolean): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("res", width, height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("vhs", if (vhs) 1f else 0f)
        return RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    companion object {
        /** Devuelve null si el dispositivo no puede compilar el shader. */
        fun create(): CrtShader? = try {
            CrtShader(RuntimeShader(CRT_AGSL))
        } catch (e: Exception) {
            null
        }
    }
}
