# Nametags

Paper 1.20.4+ plugin that shows custom nametags above players' heads.

- Money (`1.12K`, `1K`, `1M`), hearts, kills and deaths, each on its own row
- Small caps white name, no bold
- `/nametag` (alias `/nametags`) opens a GUI to toggle each row or the whole tag, applied instantly
- Money uses Vault (optional)

## Build

```
mvn package
```

The jar is created in `target/`. Pushing to GitHub also builds it automatically; download it from the Actions tab under Artifacts.
