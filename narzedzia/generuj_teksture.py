# Rysuje teksture napoju Jajrzeka (32x32) - butelka z napojem jablko-brzoskwinia
import sys
from PIL import Image

W = 32
img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
px = img.load()

def p(x, y, c):
    if 0 <= x < W and 0 <= y < W:
        px[x, y] = c

OBRYS = (40, 30, 45, 255)
SZKLO = (200, 235, 245, 150)
BLYSK = (255, 255, 255, 230)

# ksztalt butelki: szerokosc w kazdym wierszu (srodek x=15.5)
ksztalt = {}
for y in range(5, 9):  ksztalt[y] = 3          # szyjka
ksztalt[9] = 4
ksztalt[10] = 6
ksztalt[11] = 8
for y in range(12, 28): ksztalt[y] = 10        # brzuch
ksztalt[28] = 9
ksztalt[29] = 8

def zakres(y):
    s = ksztalt[y]
    return 16 - s, 16 + s - 1

# kolory napoju - gora brzoskwinia, dol jablko
def napoj(y):
    t = (y - 13) / 15
    t = max(0, min(1, t))
    a = (255, 170, 95)   # brzoskwinia
    b = (235, 70, 55)    # czerwone jablko
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

for y, s in ksztalt.items():
    x0, x1 = zakres(y)
    for x in range(x0, x1 + 1):
        p(x, y, napoj(y) if y >= 13 else SZKLO)
    # obrys
    p(x0 - 1, y, OBRYS); p(x1 + 1, y, OBRYS)
# poziome krawedzie obrysu
for y in ksztalt:
    x0, x1 = zakres(y)
    for yy in (y - 1, y + 1):
        if yy not in ksztalt:
            for x in range(x0 - 1, x1 + 2):
                p(x, yy, OBRYS)
        else:
            a0, a1 = zakres(yy)
            for x in range(x0 - 1, x1 + 2):
                if x < a0 or x > a1:
                    if px[x, yy][3] == 0:
                        p(x, yy, OBRYS)

# powierzchnia napoju (jasniejsza linia)
x0, x1 = zakres(13)
for x in range(x0, x1 + 1):
    p(x, 13, (255, 205, 150, 255))

# korek
for y in range(2, 6):
    for x in range(14, 18):
        p(x, y, (150, 95, 50, 255) if (x + y) % 3 else (120, 75, 40, 255))
for x in range(13, 19):
    p(x, 1, OBRYS)
for y in range(2, 6):
    p(13, y, OBRYS); p(18, y, OBRYS)

# etykieta - jajko na srodku (y 17..25)
for y in range(17, 26):
    for x in range(10, 22):
        p(x, y, (250, 245, 225, 255))
for x in range(10, 22):
    p(x, 16, (190, 150, 60, 255)); p(x, 26, (190, 150, 60, 255))
# jajko
jajko = [
    "..####..",
    ".######.",
    "########",
    "########",
    "########",
    ".######.",
]
for j, w in enumerate(jajko):
    for i, ch in enumerate(w):
        if ch == "#":
            p(12 + i, 18 + j, (255, 250, 235, 255))
for j, w in enumerate(jajko):
    for i, ch in enumerate(w):
        if ch != "#":
            continue
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ii, jj = i + dx, j + dy
            if not (0 <= jj < len(jajko) and 0 <= ii < 8) or jajko[jj][ii] != "#":
                p(12 + ii, 18 + jj, (150, 120, 80, 255))
# zolte "zoltko" i fale rzeki pod jajkiem
p(15, 21, (255, 200, 40, 255)); p(16, 21, (255, 200, 40, 255)); p(15, 22, (255, 180, 30, 255)); p(16, 22, (255, 180, 30, 255))
for x in range(11, 21):
    p(x, 25, (60, 140, 230, 255) if x % 2 else (100, 180, 250, 255))
# mini jablko i brzoskwinia po bokach etykiety
for (cx, cy), c in (((10, 19), (230, 50, 50, 255)), ((21, 19), (255, 160, 90, 255))):
    p(cx, cy, c); p(cx, cy + 1, c)
p(10, 18, (60, 160, 60, 255)); p(21, 18, (60, 160, 60, 255))

# blyski na szkle
for y in range(11, 16):
    p(8, y, BLYSK)
p(9, 10, BLYSK)
for y in range(27, 29):
    p(23, y, (255, 255, 255, 120))

img.save(sys.argv[1])
img.resize((256, 256), Image.NEAREST).save(sys.argv[1].replace(".png", "_podglad.png"))
