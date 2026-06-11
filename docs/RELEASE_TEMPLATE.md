# RiftForge Release Notes Template

## Version

`vX.Y.Z-alpha`

## Date

YYYY-MM-DD

## Status

Alpha / Beta

## Summary

- Short release summary.
- Who should install this build.
- Whether this is recommended for public playtesting.

## Rules Support Changes

- Turn flow:
- Rune/payment:
- Movement/showdowns:
- Combat/scoring:
- Validation/legal actions:

## Card Support Changes

- Newly supported cards:
- Partial cards improved:
- Unsupported cards now blocked:
- Banlist / legality changes:

## Known Limitations

- Rules gaps:
- Card effect gaps:
- UI/UX gaps:
- Multiplayer/session gaps:

## Download Assets

- Windows installer: `RiftForge_<version>_x64-setup.exe`
- macOS package, if published:
- Linux package, if published:
- Source archive: GitHub generated source zip/tarball

## Checksums

Generate checksums before uploading release assets.

```powershell
Get-FileHash .\RiftForge_<version>_x64-setup.exe -Algorithm SHA256
```

```bash
sha256sum RiftForge_<version>_x64-setup.exe
```

| Asset | SHA-256 |
| --- | --- |
| `RiftForge_<version>_x64-setup.exe` | TBD |

## Validation

- `npm run build`
- `cd server && mvn -q test`
- `cd server && mvn -q -DskipTests compile`
- Desktop installer smoke test:
- Server health check:

## Fan Project Notice

RiftForge is an unofficial fan-made project. It is not affiliated with,
endorsed, sponsored, or approved by Riot Games, League of Legends, Riftbound,
or UVS Games. Official rules, card text, tournament documents, banlists, and
errata remain the source of truth.
