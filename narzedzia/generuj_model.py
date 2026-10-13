# Generuje model 3D butelki Jajrzeka: atlas tekstury 32x32 + model JSON + podglad
# uzycie: python generuj_model.py <folder_wyjscia> <namespace:item/tekstura>
import json
import math
import os
import sys
from PIL import Image, ImageDraw

wyj = sys.argv[1]
tex_id = sys.argv[2]
os.makedirs(wyj, exist_ok=True)

S = 32  # 2 px na jednostke modelu
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
px = img.load()


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = c


def gradient(y, h):
    t = y / max(1, h - 1)
    a = (255, 175, 100)  # brzoskwinia
    b = (232, 68, 52)    # czerwone jablko
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


SZKLO = (205, 238, 248, 175)
SZKLO_KRAWEDZ = (150, 200, 220, 220)
BLYSK = (255, 255, 255, 235)

# A: przod/tyl brzucha z etykieta (px 0..12 x 0..16)
for y in range(16):
    for x in range(12):
        px[x, y] = gradient(y, 16)
rect(0, 0, 12, 1, (255, 215, 160, 255))           # powierzchnia napoju
rect(2, 4, 10, 13, (250, 244, 222, 255))           # etykieta
for x in range(2, 10):
    px[x, 4] = (195, 150, 60, 255)
    px[x, 12] = (195, 150, 60, 255)
jajko = [".##.", "####", "####", "####", ".##."]
for j, w in enumerate(jajko):
    for i, ch in enumerate(w):
        if ch == "#":
            px[4 + i, 5 + j] = (255, 252, 240, 255)
for j, w in enumerate(jajko):
    for i, ch in enumerate(w):
        if ch != "#":
            continue
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ii, jj = i + dx, j + dy
            if not (0 <= jj < 5 and 0 <= ii < 4) or jajko[jj][ii] != "#":
                xx, yy = 4 + ii, 5 + jj
                if 2 <= xx < 10 and 4 < yy < 12:
                    px[xx, yy] = (150, 120, 80, 255)
px[5, 7] = px[6, 7] = px[5, 8] = px[6, 8] = (255, 195, 35, 255)  # zoltko
for x in range(3, 9):
    px[x, 11] = (60, 140, 230, 255) if x % 2 else (110, 185, 250, 255)  # rzeka
px[3, 5] = (230, 50, 50, 255); px[8, 5] = (255, 160, 90, 255)          # jablko i brzoskwinia
px[3, 6] = (230, 50, 50, 255); px[8, 6] = (255, 160, 90, 255)
for y in range(1, 15):
    px[0, y] = (255, 230, 210, 255)  # blysk szkla
for y in range(16):
    px[11, y] = tuple(int(c * 0.8) for c in px[11, y][:3]) + (255,)

# B: boki brzucha (px 12..24 x 0..16)
for y in range(16):
    for x in range(12, 24):
        px[x, y] = gradient(y, 16)
rect(12, 0, 24, 1, (255, 215, 160, 255))
for y in range(2, 13):
    px[14, y] = BLYSK
    px[15, y] = (255, 235, 220, 255)
for y in range(16):
    px[23, y] = tuple(int(c * 0.8) for c in px[23, y][:3]) + (255,)

# C: gora/dol brzucha (px 0..12 x 16..28)
rect(0, 16, 12, 28, (255, 205, 150, 255))
for i in range(12):
    px[i, 16] = px[i, 27] = px[0, 16 + i] = px[11, 16 + i] = (230, 150, 100, 255)

# D: bok ramion (px 12..20 x 16..18), E: gora ramion (px 12..20 x 18..26)
rect(12, 16, 20, 18, SZKLO)
rect(12, 18, 20, 26, SZKLO)
for i in range(8):
    px[12 + i, 18] = px[12 + i, 25] = SZKLO_KRAWEDZ
    px[12, 18 + i] = px[19, 18 + i] = SZKLO_KRAWEDZ

# F: szyjka (px 24..28 x 0..6)
rect(24, 0, 28, 6, SZKLO)
for y in range(6):
    px[24, y] = BLYSK
    px[27, y] = SZKLO_KRAWEDZ

# G: bok korka (px 24..30 x 6..10), H: gora korka (px 24..30 x 10..16)
for y in range(6, 10):
    for x in range(24, 30):
        px[x, y] = (155, 100, 55, 255) if (x + y) % 3 else (120, 75, 40, 255)
for y in range(10, 16):
    for x in range(24, 30):
        px[x, y] = (170, 115, 65, 255) if (x * 3 + y) % 4 else (135, 88, 48, 255)

img.save(os.path.join(wyj, "tekstura.png"))

# ---------------- model ----------------
def face(uv):
    return {"uv": uv, "texture": "#t"}


def kostka(a, b, bok_ns, bok_ew, gora, dol=None):
    return {
        "from": a, "to": b,
        "faces": {
            "north": face(bok_ns), "south": face(bok_ns),
            "east": face(bok_ew), "west": face(bok_ew),
            "up": face(gora), "down": face(dol or gora),
        },
    }


elementy = [
    kostka([5, 0, 5], [11, 8, 11], [0, 0, 6, 8], [6, 0, 12, 8], [0, 8, 6, 14]),
    kostka([6, 8, 6], [10, 9, 10], [6, 8, 10, 9], [6, 8, 10, 9], [6, 9, 10, 13]),
    kostka([7, 9, 7], [9, 12, 9], [12, 0, 14, 3], [12, 0, 14, 3], [12, 0, 14, 2]),
    kostka([6.5, 12, 6.5], [9.5, 14, 9.5], [12, 3, 15, 5], [12, 3, 15, 5], [12, 5, 15, 8]),
]
model = {
    "textures": {"t": tex_id, "particle": tex_id},
    "elements": elementy,
    "display": {
        "gui": {"rotation": [25, -35, 0], "translation": [0, 1, 0], "scale": [0.9, 0.9, 0.9]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 1, 0], "scale": [0.9, 0.9, 0.9]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 13, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_lefthand": {"rotation": [0, -45, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
        "firstperson_righthand": {"rotation": [0, -25, 0], "translation": [1, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "firstperson_lefthand": {"rotation": [0, 25, 0], "translation": [1, 3, 0], "scale": [0.5, 0.5, 0.5]},
    },
}
with open(os.path.join(wyj, "model.json"), "w", encoding="utf-8") as f:
    json.dump(model, f, indent=1)

# ---------------- podglad (prosty renderer, widok jak w ekwipunku) ----------------
R = 512
pod = Image.new("RGBA", (R, R), (60, 60, 70, 255))
d = ImageDraw.Draw(pod, "RGBA")
rx, ry = math.radians(-25), math.radians(-35 + 180)


def proj(p):
    x, y, z = p[0] - 8, p[1] - 7, p[2] - 8
    x, z = x * math.cos(ry) - z * math.sin(ry), x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return (R / 2 + x * 26, R / 2 - y * 26, z)


def naroza(a, b, kier):
    (x0, y0, z0), (x1, y1, z1) = a, b
    # rogi: (lewy-gora, prawy-gora, prawy-dol, lewy-dol) patrzac na sciane z zewnatrz
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[kier]


jasnosc = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
quady = []
for el in elementy:
    for kier, fc in el["faces"].items():
        c = naroza(el["from"], el["to"], kier)
        u0, v0, u1, v1 = [q * 2 for q in fc["uv"]]
        nu, nv = int(round(u1 - u0)), int(round(v1 - v0))
        for j in range(nv):
            for i in range(nu):
                col = px[int(u0) + i, int(v0) + j]
                if col[3] == 0:
                    continue
                pts = []
                for (a, b) in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                    s, t = a / nu, b / nv
                    top = [c[0][k] + (c[1][k] - c[0][k]) * s for k in range(3)]
                    bot = [c[3][k] + (c[2][k] - c[3][k]) * s for k in range(3)]
                    pts.append(proj([top[k] + (bot[k] - top[k]) * t for k in range(3)]))
                z = sum(p[2] for p in pts) / 4
                k = jasnosc[kier]
                quady.append((z, [(p[0], p[1]) for p in pts],
                              (int(col[0] * k), int(col[1] * k), int(col[2] * k), col[3])))
quady.sort(key=lambda q: -q[0])
for _, pts, col in quady:
    d.polygon(pts, fill=col)
pod.save(os.path.join(wyj, "podglad_3d.png"))
print("ok")
