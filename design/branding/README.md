# Wildlife FieldOps branding

Premium raccoon mark (forest emerald on charcoal) used for:

- Adaptive launcher icon + legacy mipmaps
- Android 12+ splash (`splash_logo` + dark `splash_background`)
- Full splash art (`drawable-nodpi/splash_branding.png`) for marketing / future branded splash
- In-app `BrandMark` composable
- Notification monochrome silhouettes

## Masters

| File | Use |
|------|-----|
| `launcher_master.jpg` | Full app icon badge |
| `foreground_master.jpg` | Adaptive foreground source |
| `splash_master.jpg` | Full-bleed splash branding |

Regenerate densities:

```bash
python design/branding/process_icons.py
```

(Point script at new master images under the session folder or update paths first.)
