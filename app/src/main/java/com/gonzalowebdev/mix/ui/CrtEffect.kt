package com.gonzalowebdev.mix.ui

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Efecto de TV de tubo / VHS (AGSL, Android 13 o superior).
 * Sin bordes redondeados: trama de fósforo, halo de luz, líneas de barrido,
 * reflejo del cristal, encendido de TV y, en VHS, temblor y pérdidas de señal.
 */
private const val CRT_AGSL = """
uniform shader content;
uniform float2 res;
uniform float time;
uniform float vhs;
uniform float power;
uniform float paused;

float rand(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / res;
    float2 c = uv * 2.0 - 1.0;
    float2 sp = fragCoord;

    float open = smoothstep(0.0, 1.0, power);
    float halfH = mix(0.004, 1.2, open * open);
    if (abs(c.y) > halfH) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }

    if (vhs > 0.5) {
        float band = exp(-abs(uv.y - fract(time * 0.08)) * 38.0);
        sp.x += band * 14.0 * sin(uv.y * 90.0 + time * 18.0);
        sp.x += (rand(float2(floor(uv.y * 180.0), floor(time * 12.0))) - 0.5) * 1.5;
        if (rand(float2(floor(time * 3.0), 1.0)) > 0.93) {
            sp.x += 8.0 * sin(uv.y * 40.0 + time * 30.0);
        }
    }

    float ab = mix(1.4, 3.2, vhs);
    half4 mid = content.eval(sp) * half(0.5)
        + content.eval(sp + float2(1.0, 0.0)) * half(0.25)
        + content.eval(sp - float2(1.0, 0.0)) * half(0.25);
    half r = content.eval(sp + float2(ab, 0.0)).r;
    half b = content.eval(sp - float2(ab, 0.0)).b;
    half3 col = half3(r, mid.g, b);

    half3 glow = half3(0.0);
    glow += content.eval(sp + float2(6.0, 0.0)).rgb;
    glow += content.eval(sp - float2(6.0, 0.0)).rgb;
    glow += content.eval(sp + float2(0.0, 6.0)).rgb;
    glow += content.eval(sp - float2(0.0, 6.0)).rgb;
    glow *= half(0.25);
    half lum = dot(glow, half3(0.299, 0.587, 0.114));
    col += glow * half(smoothstep(0.55, 1.0, float(lum))) * half(0.35);

    float scan = 0.5 + 0.5 * sin(uv.y * 260.0 * 6.28318);
    col *= half(mix(0.80, 1.0, scan));

    float px = mod(fragCoord.x, 3.0);
    half3 triad = half3(px < 1.0 ? 1.0 : 0.84, (px >= 1.0 && px < 2.0) ? 1.0 : 0.84, px >= 2.0 ? 1.0 : 0.84);
    col *= triad;
    col *= half(1.08);

    float vig = 1.0 - smoothstep(0.55, 1.6, length(c));
    col *= half(mix(0.72, 1.0, vig));

    float refl = smoothstep(0.0, 1.0, 1.0 - length(uv - float2(0.22, 0.12)) * 1.7);
    col += half(refl * 0.05);

    float bar = exp(-abs(uv.y - fract(time * 0.12)) * 18.0);
    col *= half(1.0 + 0.06 * bar + 0.02 * sin(time * 40.0));

    float n = rand(sp + float2(time * 61.0, time * 37.0));
    col += half((n - 0.5) * mix(0.05, 0.12, vhs));

    if (vhs > 0.5) {
        half l = dot(col, half3(0.299, 0.587, 0.114));
        col = mix(half3(l), col, half(0.8));
        float drop = step(0.996, rand(float2(floor(uv.y * 140.0), floor(time * 8.0))));
        col += half(drop * 0.55);
    }

    if (paused > 0.5) {
        float inBand = 1.0 - step(0.045, abs(uv.y - 0.86));
        float nz = rand(float2(floor(uv.x * 90.0), floor(uv.y * 400.0 + time * 40.0)));
        col = mix(col, half3(half(nz)), half(inBand * 0.85));
    }

    col += half((1.0 - open) * 0.6);

    return half4(col, 1.0);
}
"""

@RequiresApi(33)
class CrtShader private constructor(private val shader: RuntimeShader) {

    fun effect(
        width: Float,
        height: Float,
        time: Float,
        vhs: Boolean,
        power: Float,
        paused: Boolean
    ): androidx.compose.ui.graphics.RenderEffect? = try {
        shader.setFloatUniform("res", width, height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("vhs", if (vhs) 1f else 0f)
        shader.setFloatUniform("power", power)
        shader.setFloatUniform("paused", if (paused) 1f else 0f)
        RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    } catch (e: Exception) {
        null
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
