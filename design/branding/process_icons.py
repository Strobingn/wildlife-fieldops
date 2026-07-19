from PIL import Image
from pathlib import Path
import shutil

session = Path(r"C:\Users\Austin\.grok\sessions\C%3A%5CUsers%5CAustin\019f7baf-da2c-7161-9b8b-56d8a4d17e65\images")
res = Path(r"D:\wildlife-fieldops\app\src\main\res")
masters = Path(r"D:\wildlife-fieldops\design\branding")
masters.mkdir(parents=True, exist_ok=True)

src_icon = session / "1.jpg"
src_fg = session / "2.jpg"
src_splash = session / "3.jpg"

for name, p in [
    ("launcher_master.jpg", src_icon),
    ("foreground_master.jpg", src_fg),
    ("splash_master.jpg", src_splash),
]:
    shutil.copy2(p, masters / name)


def make_transparent_black(im: Image.Image, threshold: int = 28) -> Image.Image:
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if r <= threshold and g <= threshold and b <= threshold:
                px[x, y] = (0, 0, 0, 0)
    return im


def fit_square(im: Image.Image, size: int) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    im = im.crop((left, top, left + side, top + side))
    return im.resize((size, size), Image.Resampling.LANCZOS)


# --- Launcher density icons from premium badge ---
icon_src = Image.open(src_icon)
densities = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
for dens, s in densities.items():
    out_dir = res / f"mipmap-{dens}"
    out_dir.mkdir(parents=True, exist_ok=True)
    img = fit_square(icon_src, s)
    img.save(out_dir / "ic_launcher.png", optimize=True)
    img.save(out_dir / "ic_launcher_round.png", optimize=True)
    print(f"wrote mipmap-{dens} {s}px")

# --- Adaptive foreground ---
fg_raw = Image.open(src_fg).convert("RGBA")
fg_t = make_transparent_black(fg_raw, threshold=22)
FG_SIZE = 1024
SAFE = int(FG_SIZE * 0.62)
canvas = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
bbox = fg_t.getbbox()
cropped = fg_t.crop(bbox) if bbox else fg_t
cw, ch = cropped.size
scale = min(SAFE / cw, SAFE / ch)
nw, nh = int(cw * scale), int(ch * scale)
cropped = cropped.resize((nw, nh), Image.Resampling.LANCZOS)
ox = (FG_SIZE - nw) // 2
oy = (FG_SIZE - nh) // 2
canvas.paste(cropped, (ox, oy), cropped)

(res / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
canvas.save(res / "mipmap-anydpi-v26" / "ic_launcher_foreground.png", optimize=True)
print("wrote adaptive foreground", canvas.size)

for dens, s in [
    ("mdpi", 108),
    ("hdpi", 162),
    ("xhdpi", 216),
    ("xxhdpi", 324),
    ("xxxhdpi", 432),
]:
    d = res / f"mipmap-{dens}"
    d.mkdir(parents=True, exist_ok=True)
    canvas.resize((s, s), Image.Resampling.LANCZOS).save(
        d / "ic_launcher_foreground.png", optimize=True
    )

# Splash logo
drawable = res / "drawable"
drawable.mkdir(exist_ok=True)
canvas.resize((960, 960), Image.Resampling.LANCZOS).save(
    drawable / "splash_logo.png", optimize=True
)

# Full splash branding
nodpi = res / "drawable-nodpi"
nodpi.mkdir(exist_ok=True)
sp = Image.open(src_splash).convert("RGBA")
sw = 1080
sh = int(sp.height * (sw / sp.width))
sp.resize((sw, sh), Image.Resampling.LANCZOS).save(
    nodpi / "splash_branding.png", optimize=True
)
print("wrote splash branding", sw, sh)

# Monochrome white
alpha = canvas.split()[-1]
white = Image.new("RGBA", (FG_SIZE, FG_SIZE), (255, 255, 255, 255))
mono = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
mono.paste(white, (0, 0), alpha)
mono.save(drawable / "ic_launcher_monochrome.png", optimize=True)

# Notification icons
for dens, s in {
    "mdpi": 24,
    "hdpi": 36,
    "xhdpi": 48,
    "xxhdpi": 72,
    "xxxhdpi": 96,
}.items():
    d = res / f"drawable-{dens}"
    d.mkdir(exist_ok=True)
    n = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
    sub_s = int(FG_SIZE * 0.85)
    sub = canvas.resize((sub_s, sub_s), Image.Resampling.LANCZOS)
    off = (FG_SIZE - sub_s) // 2
    n.paste(sub, (off, off), sub)
    a = n.split()[-1]
    out = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
    out.paste(Image.new("RGBA", (FG_SIZE, FG_SIZE), (255, 255, 255, 255)), (0, 0), a)
    out.resize((s, s), Image.Resampling.LANCZOS).save(
        d / "ic_notification.png", optimize=True
    )

print("DONE")
