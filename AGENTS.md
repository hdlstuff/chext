# Agent Notes

## Documentation Generation

`docs/synopsis.txt` is the source of truth for the synopsis.

For any task that changes the repository, run the synopsis generator before finishing:

```bash
python3 scripts/generate_synopsis.py
```

When changing Chext constructions, naming behavior, hierarchy/tracking behavior, or examples that affect the synopsis, update the source first:

1. Update `docs/synopsis.txt`.
2. Run `python3 scripts/generate_synopsis.py`.
3. Commit both `docs/synopsis.txt` and the generated `docs/synopsis.md`.

Do not hand-edit `docs/synopsis.md`; regenerate it from `docs/synopsis.txt`.
